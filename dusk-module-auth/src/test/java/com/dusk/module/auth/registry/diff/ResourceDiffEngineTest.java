package com.dusk.module.auth.registry.diff;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.auth.registry.SnapshotAnonymousResource;
import com.dusk.common.core.auth.registry.SnapshotResource;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.enums.ResourceDiffType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ResourceDiffEngine} 单元测试，覆盖《权限优化方案-整理版》4.1 的四类结果
 * 与 4.9 的换绑影响面。
 */
class ResourceDiffEngineTest {

    private static final String SERVICE_ID = "dusk-module-user";
    private static final String SERVICE_VERSION = "1.3.0";

    private final ResourceDiffEngine engine = new ResourceDiffEngine();

    private ResourceSnapshot snapshot(List<SnapshotResource> resources, List<SnapshotAnonymousResource> anonymous) {
        return ResourceSnapshot.of(SERVICE_ID, SERVICE_VERSION, resources, anonymous);
    }

    @Test
    @DisplayName("ADD：新出现的方法+路径生成新增项")
    void shouldDetectAddition() {
        ResourceDiffResult result = engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "system:user:query")),
                snapshot(List.of(
                        SnapshotResource.of("GET", "/users", "system:user:query"),
                        SnapshotResource.of("POST", "/users", "system:user:create")
                ), List.of()));

        assertThat(result.additions()).hasSize(1);
        assertThat(result.additions().getFirst().key()).isEqualTo(ResourceKey.of("POST", "/users"));
        assertThat(result.additions().getFirst().currentPermission()).isEqualTo("system:user:create");
        assertThat(result.additions().getFirst().previous()).isNull();
    }

    @Test
    @DisplayName("REMOVE：消失的方法+路径生成删除项（Permission 侧不受影响）")
    void shouldDetectRemoval() {
        ResourceDiffResult result = engine.diff(
                List.of(
                        ResourceBinding.of("GET", "/users", "P1"),
                        ResourceBinding.of("DELETE", "/users", "P3")
                ),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P1")), List.of()));

        assertThat(result.removals()).hasSize(1);
        ResourceDiffEntry removal = result.removals().getFirst();
        assertThat(removal.key()).isEqualTo(ResourceKey.of("DELETE", "/users"));
        assertThat(removal.previousPermission()).isEqualTo("P3");
        assertThat(removal.current()).isNull();
    }

    @Test
    @DisplayName("UPDATE：同路径同权限、仅 resourceId 变化")
    void shouldDetectMetadataUpdate() {
        ResourceDiffResult result = engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "P1", "r001")),
                snapshot(List.of(new SnapshotResource("r002", "GET", "/users", "P1")), List.of()));

        assertThat(result.updates()).hasSize(1);
        assertThat(result.updates().getFirst().key()).isEqualTo(ResourceKey.of("GET", "/users"));
    }

    @Test
    @DisplayName("UPDATE 不判定：任一侧缺少 resourceId 时不误报")
    void shouldNotReportUpdateWhenResourceIdMissing() {
        assertThat(engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "P1")),
                snapshot(List.of(new SnapshotResource("r002", "GET", "/users", "P1")), List.of())).isEmpty())
                .isTrue();

        assertThat(engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "P1", "r001")),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P1")), List.of())).isEmpty())
                .isTrue();
    }

    @Test
    @DisplayName("REBIND：同路径换绑权限，不影响运行时（由上层挂起）")
    void shouldDetectRebind() {
        ResourceDiffResult result = engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "P1")),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P9")), List.of()));

        assertThat(result.hasRebind()).isTrue();
        ResourceDiffEntry rebind = result.rebinds().getFirst();
        assertThat(rebind.previousPermission()).isEqualTo("P1");
        assertThat(rebind.currentPermission()).isEqualTo("P9");
        assertThat(rebind.rebindImpact()).isNotNull();
        assertThat(rebind.rebindImpact().losingRoleIds()).isEmpty();
        assertThat(rebind.rebindImpact().gainingRoleIds()).isEmpty();
    }

    @Test
    @DisplayName("REBIND 影响面：列出失去与获得访问权的角色，两者皆持有则不计入")
    void shouldResolveRebindImpact() {
        Map<String, Set<Long>> roleHoldings = new HashMap<>();
        roleHoldings.put("P1", Set.of(1L, 2L, 3L));
        roleHoldings.put("P9", Set.of(3L, 4L));
        PermissionRoleLookup lookup = roleHoldings::get;

        ResourceDiffResult result = engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "P1")),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P9")), List.of()),
                lookup);

        RebindImpact impact = result.rebinds().getFirst().rebindImpact();
        assertThat(impact.losingRoleIds()).containsExactly(1L, 2L);
        assertThat(impact.gainingRoleIds()).containsExactly(4L);
    }

    @Test
    @DisplayName("角色查询返回 null 或含 null 元素时按空集合容错，不阻断 Diff")
    void shouldTolerateBrokenRoleLookup() {
        ResourceDiffResult nullResult = engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "P1")),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P9")), List.of()),
                code -> null);
        assertThat(nullResult.rebinds().getFirst().rebindImpact().losingRoleIds()).isEmpty();

        ResourceDiffResult dirtyResult = engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "P1")),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P9")), List.of()),
                code -> "P1".equals(code)
                        ? new java.util.HashSet<>(java.util.Arrays.asList(7L, null))
                        : Set.of(9L));
        assertThat(dirtyResult.rebinds().getFirst().rebindImpact().losingRoleIds()).containsExactly(7L);
        assertThat(dirtyResult.rebinds().getFirst().rebindImpact().gainingRoleIds()).containsExactly(9L);
    }

    @Test
    @DisplayName("完全一致的重复注册是空 Diff（注册幂等）")
    void shouldBeIdempotentForIdenticalRegistration() {
        List<SnapshotResource> resources = List.of(
                SnapshotResource.of("GET", "/users", "P1"),
                SnapshotResource.of("POST", "/users", "P2"));

        ResourceSnapshot first = snapshot(resources, List.of());
        List<ResourceBinding> bindings = ResourceDiffEngine.toBindings(first);

        assertThat(engine.diff(bindings, first).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("匿名资源参与 Diff，与受保护资源共用一套判定")
    void shouldDiffAnonymousResources() {
        ResourceDiffResult result = engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "P1")),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P1")),
                        List.of(SnapshotAnonymousResource.of("POST", "/login"))));

        assertThat(result.additions()).hasSize(1);
        ResourceDiffEntry addition = result.additions().getFirst();
        assertThat(addition.key()).isEqualTo(ResourceKey.of("POST", "/login"));
        assertThat(addition.currentPermission()).isEqualTo(ResourceBinding.ANONYMOUS_PERMISSION);
    }

    @Test
    @DisplayName("接口由受保护改为匿名：识别为 REBIND，不允许访问控制语义静默变化")
    void shouldTreatProtectionToAnonymousAsRebind() {
        ResourceDiffResult result = engine.diff(
                List.of(ResourceBinding.of("GET", "/users", "P1", "r001")),
                snapshot(List.of(), List.of(SnapshotAnonymousResource.of("GET", "/users"))));

        assertThat(result.hasRebind()).isTrue();
        assertThat(result.rebinds().getFirst().currentPermission())
                .isEqualTo(ResourceBinding.ANONYMOUS_PERMISSION);
    }

    @Test
    @DisplayName("结果顺序稳定：先按类型分组，再按 method、path 排序")
    void shouldProduceDeterministicOrder() {
        ResourceDiffResult result = engine.diff(
                List.of(
                        ResourceBinding.of("DELETE", "/roles", "P5"),
                        ResourceBinding.of("GET", "/roles", "P4"),
                        ResourceBinding.of("GET", "/users", "P1")
                ),
                snapshot(List.of(
                        SnapshotResource.of("GET", "/users", "P9"),
                        SnapshotResource.of("GET", "/accounts", "P6")
                ), List.of()));

        assertThat(result.entries()).extracting(ResourceDiffEntry::toString).containsExactly(
                "ADD GET /accounts -> P6",
                "REMOVE DELETE /roles (原 P5)",
                "REMOVE GET /roles (原 P4)",
                "REBIND GET /users : P1 -> P9");
        assertThat(result.totalChanges()).isEqualTo(4);
    }

    @Test
    @DisplayName("基准集合中出现重复资源时直接失败（数据损坏不应被静默吞掉）")
    void shouldRejectDuplicatedPreviousBindings() {
        List<ResourceBinding> duplicated = List.of(
                ResourceBinding.of("GET", "/users", "P1"),
                ResourceBinding.of("get", "/users", "P2"));

        assertThatThrownBy(() -> engine.diff(duplicated,
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P1")), List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("基准绑定集合中资源重复");
    }

    @Test
    @DisplayName("基准集合含 null 元素时直接失败")
    void shouldRejectNullBinding() {
        assertThatThrownBy(() -> engine.diff(java.util.Arrays.asList(ResourceBinding.of("GET", "/a", "P1"), null),
                snapshot(List.of(), List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("基准绑定集合中存在 null 元素");
    }

    @Test
    @DisplayName("snapshot 为空时直接失败")
    void shouldRejectNullSnapshot() {
        assertThatThrownBy(() -> engine.diff(List.of(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("snapshot 不能为空");
    }

    @Test
    @DisplayName("空基准 + 空快照 = 空 Diff")
    void shouldReturnEmptyForBothEmpty() {
        ResourceDiffResult result = engine.diff(List.of(), snapshot(List.of(), List.of()));

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.entries()).isEqualTo(ResourceDiffResult.empty().entries());
        assertThat(result.ofType(ResourceDiffType.ADD)).isEmpty();
    }
}
