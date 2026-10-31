package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.auth.registry.SnapshotResource;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.diff.*;
import com.dusk.module.auth.registry.enums.ResourceDiffType;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RegistryApplyPlanner} 的防御分支补充测试：覆盖 {@code RegistryApplyPlannerTest}
 * 未触达的空输入、非法集合、换绑基准缺失与元数据比较分支。
 */
class RegistryApplyPlannerCoverageTest {

    private static final String SERVICE_ID = "dusk-module-user";

    private final RegistryApplyPlanner planner = new RegistryApplyPlanner();

    private static ResourceSnapshot snapshot(List<SnapshotResource> resources) {
        return ResourceSnapshot.of(SERVICE_ID, "1.0.0", resources, List.of());
    }

    private static List<RegisteredResource> registered(ResourceBinding binding) {
        return List.of(RegisteredResource.of(binding, ResourceStatus.ACTIVE));
    }

    @Test
    @DisplayName("plan：snapshot 为 null 时立即失败")
    void plan_nullSnapshot() {
        assertThatThrownBy(() -> planner.plan(List.of(), List.of(), null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("snapshot 不能为空");
    }

    @Test
    @DisplayName("plan：baseBindings 为 null 时按空基准处理")
    void plan_nullBaseBindings() {
        RegistryApplyPlan plan = planner.plan(
                List.of(),
                null,
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P1"))),
                null);

        assertThat(plan.ofType(ResourceDiffType.ADD)).hasSize(1);
        assertThat(plan.touchedPermissions()).containsExactly("P1");
    }

    @Test
    @DisplayName("plan：existingResources 为 null 时按空集合处理")
    void plan_nullExistingResources() {
        RegistryApplyPlan plan = planner.plan(
                null,
                List.of(),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P1"))),
                null);

        assertThat(plan.ofType(ResourceDiffType.ADD)).hasSize(1);
    }

    @Test
    @DisplayName("plan：existingResources 含 null 元素时报错")
    void plan_existingResourcesContainsNull() {
        assertThatThrownBy(() -> planner.plan(
                Arrays.asList((RegisteredResource) null),
                List.of(),
                snapshot(List.of()),
                null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("目标 Release 已有资源中存在 null 元素");
    }

    @Test
    @DisplayName("plan：baseBindings 含 null 元素时报错")
    void plan_baseBindingsContainsNull() {
        assertThatThrownBy(() -> planner.plan(
                List.of(),
                Arrays.asList((ResourceBinding) null),
                snapshot(List.of()),
                null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Diff 基准中存在 null 元素");
    }

    @Test
    @DisplayName("plan：baseBindings 重复 (method, path) 时报错")
    void plan_duplicatedBaseBinding() {
        ResourceBinding binding = ResourceBinding.of("GET", "/users", "P1");

        assertThatThrownBy(() -> planner.plan(
                List.of(),
                List.of(binding, binding),
                snapshot(List.of()),
                null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Diff 基准中存在重复资源");
    }

    @Test
    @DisplayName("plan：Diff 报换绑但基准中缺少该键时报错")
    void plan_rebindMissingBaseBinding() {
        ResourceDiffEngine diffEngine = mock(ResourceDiffEngine.class);
        ResourceBinding previous = ResourceBinding.of("GET", "/users", "P1");
        ResourceBinding current = ResourceBinding.of("GET", "/users", "P2");
        ResourceDiffEntry rebind = ResourceDiffEntry.rebind(previous, current,
                RebindImpact.unknown("P1", "P2"));
        when(diffEngine.diff(any(), any(ResourceSnapshot.class), any()))
                .thenReturn(new ResourceDiffResult(List.of(rebind)));
        RegistryApplyPlanner mockedPlanner = new RegistryApplyPlanner(diffEngine);

        assertThatThrownBy(() -> mockedPlanner.plan(
                List.of(),
                List.of(),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P2"))),
                null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("换绑资源在 Diff 基准中缺失");
    }

    @Test
    @DisplayName("plan：快照缺少 resourceId 时不判定为元数据变化")
    void plan_currentResourceIdMissing() {
        ResourceBinding previous = ResourceBinding.of("GET", "/users", "P1", "r001");

        RegistryApplyPlan plan = planner.plan(
                registered(previous),
                List.of(previous),
                snapshot(List.of(SnapshotResource.of("GET", "/users", "P1"))),
                null);

        assertThat(plan.isEmpty()).isTrue();
        assertThat(plan.mutations()).isEmpty();
    }

    @Test
    @DisplayName("plan：两侧 resourceId 相同时不判定为元数据变化")
    void plan_sameResourceId() {
        ResourceBinding previous = ResourceBinding.of("GET", "/users", "P1", "r001");
        ResourceBinding current = ResourceBinding.of("GET", "/users", "P1", "r001");

        RegistryApplyPlan plan = planner.plan(
                registered(previous),
                List.of(previous),
                snapshot(List.of(new SnapshotResource("r001", "GET", "/users", "P1"))),
                null);

        assertThat(plan.mutations()).isEmpty();
        assertThat(plan.touchedPermissions()).isEmpty();
    }

    @Test
    @DisplayName("普通变更：匿名资源不进入 touchedPermissions")
    void plan_anonymousOnly() {
        RegistryApplyPlan plan = planner.plan(
                List.of(),
                List.of(),
                snapshot(List.of()),
                null);

        assertThat(plan.isEmpty()).isTrue();
        assertThat(plan.touchedPermissions()).isEmpty();
    }
}
