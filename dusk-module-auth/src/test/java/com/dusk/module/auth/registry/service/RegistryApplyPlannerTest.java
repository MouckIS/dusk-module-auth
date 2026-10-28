package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.auth.registry.SnapshotAnonymousResource;
import com.dusk.common.core.auth.registry.SnapshotResource;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.diff.ResourceBinding;
import com.dusk.module.auth.registry.enums.ResourceDiffType;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RegistryApplyPlanner} 单元测试，覆盖《权限优化方案-整理版》3.4 的落库口径、
 * 4.1 的四类 Diff、4.5 的多版本并存、4.9 的换绑挂起与 4.11 的回滚恢复。
 *
 * <p>重点验证两条最容易写错、又只能靠发布才能发现的行为：</p>
 * <ol>
 *   <li>新 Release 的资源集合是<b>快照本身</b>，与上一版本相同的接口也必须落库；</li>
 *   <li>基于「上一版本」得出的 REMOVE <b>不能</b>施加到旧 Release 上（旧实例还在跑）。</li>
 * </ol>
 */
class RegistryApplyPlannerTest {

    private static final String SERVICE_ID = "dusk-module-user";

    private final RegistryApplyPlanner planner = new RegistryApplyPlanner();

    private static ResourceSnapshot snapshot(String serviceVersion,
                                             List<SnapshotResource> resources,
                                             List<SnapshotAnonymousResource> anonymous) {
        return ResourceSnapshot.of(SERVICE_ID, serviceVersion, resources, anonymous);
    }

    private static List<RegisteredResource> registered(ResourceBinding binding, ResourceStatus status) {
        return List.of(RegisteredResource.of(binding, status));
    }

    private static Map<String, ResourceMutation> byRoute(RegistryApplyPlan plan) {
        return plan.mutations().stream().collect(Collectors.toMap(
                ResourceMutation::asText, Function.identity(), (left, right) -> left));
    }

    @Test
    @DisplayName("新 Release：与上一版本相同的接口也必须在本 Release 落库（快照物化，而不是只落 Diff）")
    void shouldMaterializeWholeSnapshotForNewRelease() {
        List<ResourceBinding> predecessor = List.of(ResourceBinding.of("GET", "/users", "P1"));

        RegistryApplyPlan plan = planner.plan(
                List.of(),
                predecessor,
                snapshot("1.3.0", List.of(
                        SnapshotResource.of("GET", "/users", "P1"),
                        SnapshotResource.of("POST", "/users", "P2")), List.of()),
                null);

        assertThat(plan.ofType(ResourceDiffType.ADD)).hasSize(2);
        assertThat(plan.mutations()).allMatch(mutation -> mutation.targetStatus() == ResourceStatus.ACTIVE);
        assertThat(plan.touchedPermissions()).containsExactlyInAnyOrder("P1", "P2");
    }

    @Test
    @DisplayName("新 Release：相对上一版本消失的接口不落库废弃（旧 Release 还在服务，4.5）")
    void shouldNotDeprecateResourcesOfPreviousRelease() {
        List<ResourceBinding> predecessor = List.of(
                ResourceBinding.of("GET", "/users", "P1"),
                ResourceBinding.of("DELETE", "/users", "P3"));

        RegistryApplyPlan plan = planner.plan(
                List.of(),
                predecessor,
                snapshot("1.3.0", List.of(SnapshotResource.of("GET", "/users", "P1")), List.of()),
                null);

        assertThat(plan.diff().removals()).hasSize(1);
        assertThat(plan.ofType(ResourceDiffType.REMOVE)).isEmpty();
        assertThat(plan.touchedPermissions()).containsExactly("P1");
    }

    @Test
    @DisplayName("同版本重注册：消失的接口标记为 DEPRECATED，且其权限码进入重算集合")
    void shouldDeprecateMissingResourceOfSameRelease() {
        ResourceBinding kept = ResourceBinding.of("GET", "/users", "P1");
        ResourceBinding removed = ResourceBinding.of("DELETE", "/users", "P3");

        RegistryApplyPlan plan = planner.plan(
                List.of(RegisteredResource.of(kept, ResourceStatus.ACTIVE),
                        RegisteredResource.of(removed, ResourceStatus.ACTIVE)),
                List.of(kept, removed),
                snapshot("1.3.0", List.of(SnapshotResource.of("GET", "/users", "P1")), List.of()),
                null);

        Map<String, ResourceMutation> mutations = byRoute(plan);
        // 未变化的资源不产出变更；只有消失的那一条需要落库为 DEPRECATED。
        assertThat(mutations).containsOnlyKeys("DELETE /users");
        assertThat(mutations.get("DELETE /users").type()).isEqualTo(ResourceDiffType.REMOVE);
        assertThat(mutations.get("DELETE /users").targetStatus()).isEqualTo(ResourceStatus.DEPRECATED);
        assertThat(plan.touchedPermissions()).containsExactly("P3");
    }

    @Test
    @DisplayName("换绑（同 Release）：生效绑定保持旧值，新值挂起等待管理员确认（4.9）")
    void shouldSuspendRebindOnSameRelease() {
        ResourceBinding binding = ResourceBinding.of("GET", "/users", "P1");

        RegistryApplyPlan plan = planner.plan(
                registered(binding, ResourceStatus.ACTIVE),
                List.of(binding),
                snapshot("1.3.0", List.of(SnapshotResource.of("GET", "/users", "P9")), List.of()),
                null);

        ResourceMutation mutation = plan.mutations().getFirst();
        assertThat(mutation.type()).isEqualTo(ResourceDiffType.REBIND);
        assertThat(mutation.permissionCode()).isEqualTo("P1");
        assertThat(mutation.pendingPermissionCode()).isEqualTo("P9");
        assertThat(mutation.targetStatus()).isEqualTo(ResourceStatus.PENDING_REBIND);
        // 候选权限也要进入待重算集合：管理员需要在换绑待办里看到它（4.9 的影响面清单）。
        assertThat(plan.touchedPermissions()).containsExactlyInAnyOrder("P1", "P9");
    }

    @Test
    @DisplayName("换绑（新 Release）：沿用上一版本已生效的绑定，而不是让新值直接生效")
    void shouldSuspendRebindOnNewRelease() {
        ResourceBinding predecessor = ResourceBinding.of("GET", "/users", "P1");

        RegistryApplyPlan plan = planner.plan(
                List.of(),
                List.of(predecessor),
                snapshot("1.3.0", List.of(SnapshotResource.of("GET", "/users", "P9")), List.of()),
                null);

        ResourceMutation mutation = plan.mutations().getFirst();
        assertThat(mutation.type()).isEqualTo(ResourceDiffType.REBIND);
        assertThat(mutation.permissionCode()).isEqualTo("P1");
        assertThat(mutation.pendingPermissionCode()).isEqualTo("P9");
    }

    @Test
    @DisplayName("开发者回退换绑：挂起候选作废，资源恢复 ACTIVE")
    void shouldClearPendingRebindWhenSnapshotReverts() {
        ResourceBinding binding = ResourceBinding.of("GET", "/users", "P1");

        RegistryApplyPlan plan = planner.plan(
                registered(binding, ResourceStatus.PENDING_REBIND),
                List.of(binding),
                snapshot("1.3.0", List.of(SnapshotResource.of("GET", "/users", "P1")), List.of()),
                null);

        ResourceMutation mutation = plan.mutations().getFirst();
        assertThat(mutation.type()).isEqualTo(ResourceDiffType.UPDATE);
        assertThat(mutation.targetStatus()).isEqualTo(ResourceStatus.ACTIVE);
        assertThat(mutation.pendingPermissionCode()).isNull();
        assertThat(mutation.permissionCode()).isEqualTo("P1");
    }

    @Test
    @DisplayName("回滚：已废弃的接口重新被快照声明则恢复 ACTIVE，无需重建（4.11）")
    void shouldRestoreDeprecatedResourceOnRollback() {
        ResourceBinding binding = ResourceBinding.of("DELETE", "/users", "P3");

        RegistryApplyPlan plan = planner.plan(
                registered(binding, ResourceStatus.DEPRECATED),
                List.of(binding),
                snapshot("1.2.0", List.of(SnapshotResource.of("DELETE", "/users", "P3")), List.of()),
                null);

        ResourceMutation mutation = plan.mutations().getFirst();
        assertThat(mutation.targetStatus()).isEqualTo(ResourceStatus.ACTIVE);
        assertThat(mutation.type()).isEqualTo(ResourceDiffType.UPDATE);
    }

    @Test
    @DisplayName("匿名资源：以 ANONYMOUS 绑定落库，且不产生权限点")
    void shouldMaterializeAnonymousResourceWithoutPermission() {
        RegistryApplyPlan plan = planner.plan(
                List.of(),
                List.of(),
                snapshot("1.0.0", List.of(), List.of(SnapshotAnonymousResource.of("POST", "/login"))),
                null);

        ResourceMutation mutation = plan.mutations().getFirst();
        assertThat(mutation.anonymous()).isTrue();
        assertThat(mutation.targetStatus()).isEqualTo(ResourceStatus.ACTIVE);
        assertThat(plan.touchedPermissions()).isEmpty();
    }

    @Test
    @DisplayName("接口从受保护改为匿名同样按换绑处理，不允许访问控制语义静默变化")
    void shouldTreatProtectedToAnonymousAsRebind() {
        ResourceBinding binding = ResourceBinding.of("GET", "/open", "P1");

        RegistryApplyPlan plan = planner.plan(
                registered(binding, ResourceStatus.ACTIVE),
                List.of(binding),
                snapshot("1.3.0", List.of(), List.of(SnapshotAnonymousResource.of("GET", "/open"))),
                null);

        ResourceMutation mutation = plan.mutations().getFirst();
        assertThat(mutation.type()).isEqualTo(ResourceDiffType.REBIND);
        assertThat(mutation.permissionCode()).isEqualTo("P1");
        assertThat(mutation.pendingPermissionCode()).isEqualTo(ResourceBinding.ANONYMOUS_PERMISSION);
    }

    @Test
    @DisplayName("仅元数据变化（resourceId 变化）产出 UPDATE，绑定不变")
    void shouldDetectMetadataOnlyChange() {
        ResourceBinding before = ResourceBinding.of("GET", "/users", "P1", "r001");
        ResourceBinding after = ResourceBinding.of("GET", "/users", "P1", "r002");

        RegistryApplyPlan plan = planner.plan(
                registered(before, ResourceStatus.ACTIVE),
                List.of(before),
                snapshot("1.3.0", List.of(new SnapshotResource("r002", "GET", "/users", "P1")), List.of()),
                null);

        ResourceMutation mutation = plan.mutations().getFirst();
        assertThat(mutation.type()).isEqualTo(ResourceDiffType.UPDATE);
        assertThat(mutation.resourceId()).isEqualTo("r002");
        assertThat(mutation.targetStatus()).isEqualTo(ResourceStatus.ACTIVE);
    }

    @Test
    @DisplayName("重复上报同一快照不产出任何变更（幂等）")
    void shouldProduceNoMutationForIdenticalSnapshot() {
        ResourceBinding binding = ResourceBinding.of("GET", "/users", "P1");

        RegistryApplyPlan plan = planner.plan(
                registered(binding, ResourceStatus.ACTIVE),
                List.of(binding),
                snapshot("1.3.0", List.of(SnapshotResource.of("GET", "/users", "P1")), List.of()),
                null);

        assertThat(plan.isEmpty()).isTrue();
        assertThat(plan.diff().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("空快照（无任何资源）是合法的：清空该 Release 的全部资源")
    void shouldDeprecateAllResourcesForEmptySnapshot() {
        ResourceBinding binding = ResourceBinding.of("GET", "/users", "P1");

        RegistryApplyPlan plan = planner.plan(
                registered(binding, ResourceStatus.ACTIVE),
                List.of(binding),
                snapshot("1.1.0", List.of(), List.of()),
                null);

        assertThat(plan.ofType(ResourceDiffType.REMOVE)).hasSize(1);
        assertThat(plan.touchedPermissions()).containsExactly("P1");
    }

    @Test
    @DisplayName("既有资源集合出现重复 (method, path) 时报错（5.10 要求路径精确唯一）")
    void shouldRejectDuplicatedExistingResource() {
        ResourceBinding binding = ResourceBinding.of("GET", "/users", "P1");

        assertThatThrownBy(() -> planner.plan(
                List.of(RegisteredResource.of(binding, ResourceStatus.ACTIVE),
                        RegisteredResource.of(binding, ResourceStatus.ACTIVE)),
                List.of(binding),
                snapshot("1.3.0", List.of(SnapshotResource.of("GET", "/users", "P1")), List.of()),
                null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重复资源");
    }
}
