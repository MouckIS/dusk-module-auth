package com.dusk.module.auth.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.cache.IFeatureCache;
import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.dto.feature.FeatureValueInput;
import com.dusk.module.auth.entity.FeatureValue;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.repository.IFeatureValueRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link FeatureServiceImpl} 单元测试，覆盖特性子系统（feature/）的读取与合并逻辑：
 *
 * <ul>
 *   <li>默认特性 + 版本（Edition）特性 + 租户特性的三层覆盖顺序；</li>
 *   <li>「等于默认值即删除记录、否则新增或更新」的写入口径；</li>
 *   <li>租户上下文缺失、租户不存在、租户无版本等异常分支；</li>
 *   <li>{@code getFeatureValue} 的租户 → 版本 → null 回退链。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeatureServiceImplTest {

    private static final Long TENANT_ID = 1L;
    private static final Long EDITION_ID = 10L;

    @Mock
    private IFeatureValueRepository featureValueRepository;
    @Mock
    private ITenantRepository tenantRepository;
    @Mock
    private IFeatureCache featureCache;

    private FeatureServiceImpl service;

    private static TenantFeature defaultFeature(String name, String defaultValue) {
        TenantFeature feature = new TenantFeature();
        feature.setName(name);
        feature.setDefaultValue(defaultValue);
        return feature;
    }

    private static FeatureValue featureValue(String name, String value) {
        FeatureValue featureValue = new FeatureValue();
        featureValue.setName(name);
        featureValue.setValue(value);
        return featureValue;
    }

    // ---------------------------------------------------------------- 辅助构造

    private static FeatureValueInput input(String name, String value) {
        FeatureValueInput input = new FeatureValueInput();
        input.setName(name);
        input.setValue(value);
        return input;
    }

    private static Tenant tenant(Long editionId) {
        Tenant tenant = new Tenant();
        if (editionId != null) {
            SubscribableEdition edition = new SubscribableEdition();
            edition.setId(editionId);
            tenant.setEdition(edition);
        }
        return tenant;
    }

    @BeforeEach
    void setUp() {
        service = new FeatureServiceImpl();
        ReflectionTestUtils.setField(service, "featureValueRepository", featureValueRepository);
        ReflectionTestUtils.setField(service, "tenantRepository", tenantRepository);
        ReflectionTestUtils.setField(service, "featureCache", featureCache);
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ---------------------------------------------------------------- 读取

    @Test
    @DisplayName("getTenantFeatures：缺少租户上下文时返回空 Map")
    void getTenantFeaturesWithoutTenantContext() {
        assertThat(service.getTenantFeatures()).isEmpty();
    }

    @Test
    @DisplayName("getTenantFeatures：按特性名折叠为 {value: 值} 结构")
    void getTenantFeaturesMapsValues() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(null)));
        when(featureCache.getDefaultFeatureList())
                .thenReturn(List.of(defaultFeature("f1", "d1"), defaultFeature("f2", "d2")));
        when(featureValueRepository.findAllByTenantId(TENANT_ID))
                .thenReturn(List.of(featureValue("f1", "t1")));

        Map<String, Map<String, String>> features = service.getTenantFeatures();

        assertThat(features).containsOnlyKeys("f1", "f2");
        assertThat(features.get("f1")).containsEntry("value", "t1");
        assertThat(features.get("f2")).containsEntry("value", "d2");
    }

    @Test
    @DisplayName("getTenantFeaturesForEdit：tenantId 为空时返回空列表")
    void getTenantFeaturesForEditWithoutTenantId() {
        assertThat(service.getTenantFeaturesForEdit(null)).isEmpty();
    }

    @Test
    @DisplayName("getTenantFeaturesForEdit：租户不存在时抛业务异常")
    void getTenantFeaturesForEditThrowsWhenTenantMissing() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTenantFeaturesForEdit(TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未找到租户");
    }

    @Test
    @DisplayName("getTenantFeaturesForEdit：无版本时仅合并默认特性与租户特性")
    void getTenantFeaturesForEditWithoutEdition() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(null)));
        when(featureCache.getDefaultFeatureList())
                .thenReturn(List.of(defaultFeature("f1", "d1"), defaultFeature("f2", "d2")));
        when(featureValueRepository.findAllByTenantId(TENANT_ID))
                .thenReturn(List.of(featureValue("f1", "t1")));

        List<TenantFeature> features = service.getTenantFeaturesForEdit(TENANT_ID);

        assertThat(features).extracting(TenantFeature::getName).containsExactly("f1", "f2");
        assertThat(features.get(0).getFeatureValue()).isEqualTo("t1");
        assertThat(features.get(1).getFeatureValue()).isEqualTo("d2");
        verify(featureValueRepository, never()).findAllByEditionId(any());
    }

    @Test
    @DisplayName("getTenantFeaturesForEdit：版本特性覆盖默认值，租户特性再覆盖版本值")
    void getTenantFeaturesForEditMergesEditionDefaults() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(EDITION_ID)));
        when(featureCache.getDefaultFeatureList())
                .thenReturn(List.of(defaultFeature("f1", "d1"), defaultFeature("f2", "d2"),
                        defaultFeature("f3", "d3")));
        when(featureValueRepository.findAllByTenantId(TENANT_ID))
                .thenReturn(List.of(featureValue("f2", "tenant-value")));
        when(featureValueRepository.findAllByEditionId(EDITION_ID))
                .thenReturn(List.of(featureValue("f1", "edition-value"), featureValue("unknown", "ignored")));

        List<TenantFeature> features = service.getTenantFeaturesForEdit(TENANT_ID);

        assertThat(features).hasSize(3);
        assertThat(features.get(0).getDefaultValue()).isEqualTo("edition-value");
        assertThat(features.get(0).getFeatureValue()).isEqualTo("edition-value");
        assertThat(features.get(1).getDefaultValue()).isEqualTo("d2");
        assertThat(features.get(1).getFeatureValue()).isEqualTo("tenant-value");
        assertThat(features.get(2).getFeatureValue()).isEqualTo("d3");
    }

    @Test
    @DisplayName("getTenantFeaturesByEdition：版本特性覆盖默认值")
    void getTenantFeaturesByEdition() {
        when(featureCache.getDefaultFeatureList())
                .thenReturn(List.of(defaultFeature("f1", "d1"), defaultFeature("f2", "d2")));
        when(featureValueRepository.findAllByEditionId(EDITION_ID))
                .thenReturn(List.of(featureValue("f2", "e2")));

        List<TenantFeature> features = service.getTenantFeaturesByEdition(EDITION_ID);

        assertThat(features).hasSize(2);
        assertThat(features.get(0).getFeatureValue()).isEqualTo("d1");
        assertThat(features.get(1).getFeatureValue()).isEqualTo("e2");
    }

    @Test
    @DisplayName("getDefaultFeatures：直接返回缓存中的默认特性列表")
    void getDefaultFeaturesDelegatesToCache() {
        List<TenantFeature> defaults = List.of(defaultFeature("f1", "d1"));
        when(featureCache.getDefaultFeatureList()).thenReturn(defaults);

        assertThat(service.getDefaultFeatures()).isSameAs(defaults);
    }

    // ---------------------------------------------------------------- 写入：版本特性

    @Test
    @DisplayName("setEditionFeatures：委托给 setEditionFeaturesList")
    void setEditionFeaturesDelegates() {
        when(featureValueRepository.findAllByEditionId(EDITION_ID)).thenReturn(List.of());
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.setEditionFeatures(EDITION_ID, List.of(input("f1", "d1")));

        verify(featureValueRepository).findAllByEditionId(EDITION_ID);
    }

    @Test
    @DisplayName("setEditionFeaturesList：值等于默认值时删除已有版本记录")
    void setEditionFeaturesDeletesRecordEqualsDefault() {
        FeatureValue existing = featureValue("f1", "old");
        when(featureValueRepository.findAllByEditionId(EDITION_ID)).thenReturn(new ArrayList<>(List.of(existing)));
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.setEditionFeaturesList(EDITION_ID, List.of(input("f1", "d1")));

        verify(featureValueRepository).delete(existing);
        verify(featureValueRepository, never()).save(any(FeatureValue.class));
    }

    @Test
    @DisplayName("setEditionFeaturesList：值不同于默认值且已有记录时更新原记录")
    void setEditionFeaturesUpdatesExistingRecord() {
        FeatureValue existing = featureValue("f1", "old");
        when(featureValueRepository.findAllByEditionId(EDITION_ID)).thenReturn(new ArrayList<>(List.of(
                featureValue("other", "x"), existing)));
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.setEditionFeaturesList(EDITION_ID, List.of(input("f1", "new")));

        assertThat(existing.getValue()).isEqualTo("new");
        verify(featureValueRepository).save(existing);
    }

    @Test
    @DisplayName("setEditionFeaturesList：值不同于默认值且无记录时新增并写入 editionId")
    void setEditionFeaturesCreatesRecord() {
        when(featureValueRepository.findAllByEditionId(EDITION_ID)).thenReturn(new ArrayList<>());
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.setEditionFeaturesList(EDITION_ID, List.of(input("f1", "new")));

        ArgumentCaptor<FeatureValue> captor = ArgumentCaptor.forClass(FeatureValue.class);
        verify(featureValueRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("f1");
        assertThat(captor.getValue().getValue()).isEqualTo("new");
        assertThat(captor.getValue().getEditionId()).isEqualTo(EDITION_ID);
    }

    @Test
    @DisplayName("setEditionFeaturesList：特性名不在默认列表中时既不删除也不保存")
    void setEditionFeaturesIgnoresUnknownFeature() {
        when(featureValueRepository.findAllByEditionId(EDITION_ID))
                .thenReturn(new ArrayList<>(List.of(featureValue("f1", "old"))));
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.setEditionFeaturesList(EDITION_ID, List.of(
                input("ghost", "v"),
                input("f1", "d1")));

        verify(featureValueRepository).delete(any(FeatureValue.class));
        verify(featureValueRepository, never()).save(any(FeatureValue.class));
    }

    @Test
    @DisplayName("setEditionFeaturesList：值等于默认值但库里没有同名记录时不删除任何数据")
    void setEditionFeaturesDeletesNothingWhenNoMatchingRow() {
        when(featureValueRepository.findAllByEditionId(EDITION_ID))
                .thenReturn(new ArrayList<>(List.of(featureValue("other", "x"))));
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.setEditionFeaturesList(EDITION_ID, List.of(input("f1", "d1")));

        verify(featureValueRepository, never()).delete(any(FeatureValue.class));
        verify(featureValueRepository, never()).save(any(FeatureValue.class));
    }

    // ---------------------------------------------------------------- 写入：租户特性

    @Test
    @DisplayName("updateTenantFeatures：委托给 updateTenantFeaturesList")
    void updateTenantFeaturesDelegates() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(EDITION_ID)));
        when(featureValueRepository.findAllByTenantId(TENANT_ID)).thenReturn(new ArrayList<>());
        when(featureValueRepository.findAllByEditionId(EDITION_ID)).thenReturn(List.of());
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.updateTenantFeatures(TENANT_ID, List.of(input("f1", "d1")));

        verify(featureValueRepository).findAllByTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("updateTenantFeaturesList：租户不存在时抛业务异常")
    void updateTenantFeaturesThrowsWhenTenantMissing() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTenantFeaturesList(TENANT_ID, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未找到租户");
    }

    @Test
    @DisplayName("updateTenantFeaturesList：租户未定义版本时拒绝更新")
    void updateTenantFeaturesThrowsWhenEditionMissing() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(null)));
        when(featureValueRepository.findAllByTenantId(TENANT_ID)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> service.updateTenantFeaturesList(TENANT_ID, List.of(input("f1", "v"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该租户未定义版本");
    }

    @Test
    @DisplayName("updateTenantFeaturesList：值等于版本默认值 → 删除租户记录")
    void updateTenantFeaturesDeletesRecordEqualsEditionDefault() {
        FeatureValue existing = featureValue("f1", "old");
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(EDITION_ID)));
        when(featureValueRepository.findAllByTenantId(TENANT_ID)).thenReturn(new ArrayList<>(List.of(
                featureValue("other", "x"), existing)));
        when(featureValueRepository.findAllByEditionId(EDITION_ID))
                .thenReturn(List.of(featureValue("f1", "edition-default")));
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.updateTenantFeaturesList(TENANT_ID, List.of(input("f1", "edition-default")));

        verify(featureValueRepository).delete(existing);
        verify(featureValueRepository, never()).save(any(FeatureValue.class));
    }

    @Test
    @DisplayName("updateTenantFeaturesList：覆盖版本默认值且已有记录 → 更新原记录")
    void updateTenantFeaturesUpdatesExistingRecord() {
        FeatureValue existing = featureValue("f1", "old");
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(EDITION_ID)));
        when(featureValueRepository.findAllByTenantId(TENANT_ID)).thenReturn(new ArrayList<>(List.of(
                featureValue("other", "x"), existing)));
        when(featureValueRepository.findAllByEditionId(EDITION_ID)).thenReturn(List.of());
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.updateTenantFeaturesList(TENANT_ID, List.of(input("f1", "custom")));

        assertThat(existing.getValue()).isEqualTo("custom");
        verify(featureValueRepository).save(existing);
    }

    @Test
    @DisplayName("updateTenantFeaturesList：覆盖版本默认值且无记录 → 新增并写入 tenantId")
    void updateTenantFeaturesCreatesRecord() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(EDITION_ID)));
        when(featureValueRepository.findAllByTenantId(TENANT_ID)).thenReturn(new ArrayList<>());
        when(featureValueRepository.findAllByEditionId(EDITION_ID))
                .thenReturn(List.of(featureValue("f1", "edition-default")));
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.updateTenantFeaturesList(TENANT_ID, List.of(input("f1", "custom")));

        ArgumentCaptor<FeatureValue> captor = ArgumentCaptor.forClass(FeatureValue.class);
        verify(featureValueRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(captor.getValue().getValue()).isEqualTo("custom");
    }

    @Test
    @DisplayName("updateTenantFeaturesList：未知特性名被忽略")
    void updateTenantFeaturesIgnoresUnknownFeature() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(EDITION_ID)));
        when(featureValueRepository.findAllByTenantId(TENANT_ID))
                .thenReturn(new ArrayList<>(List.of(featureValue("f1", "old"))));
        when(featureValueRepository.findAllByEditionId(EDITION_ID)).thenReturn(List.of());
        when(featureCache.getDefaultFeatureList()).thenReturn(List.of(defaultFeature("f1", "d1")));

        service.updateTenantFeaturesList(TENANT_ID, List.of(input("ghost", "v")));

        verify(featureValueRepository, never()).save(any(FeatureValue.class));
        verify(featureValueRepository, never()).delete(any(FeatureValue.class));
    }

    @Test
    @DisplayName("resetTenantSpecificFeatures：物理删除租户特性记录")
    void resetTenantSpecificFeaturesDeletesByTenant() {
        service.resetTenantSpecificFeatures(TENANT_ID);

        verify(featureValueRepository).deleteByTenantId(TENANT_ID);
    }

    // ---------------------------------------------------------------- getFeatureValue 回退链

    @Test
    @DisplayName("getFeatureValue：租户配置命中时直接返回，不再查版本")
    void getFeatureValueReturnsTenantValue() {
        when(featureValueRepository.getFeatureValueByTenant(TENANT_ID, "f1")).thenReturn(featureValue("f1", "t1"));

        assertThat(service.getFeatureValue(TENANT_ID, "f1")).isEqualTo("t1");
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getFeatureValue：租户无配置时回退到版本默认值")
    void getFeatureValueFallsBackToEdition() {
        when(featureValueRepository.getFeatureValueByTenant(TENANT_ID, "f1")).thenReturn(null);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(EDITION_ID)));
        when(featureValueRepository.getFeatureValueByEdition(EDITION_ID, "f1")).thenReturn(featureValue("f1", "e1"));

        assertThat(service.getFeatureValue(TENANT_ID, "f1")).isEqualTo("e1");
    }

    @Test
    @DisplayName("getFeatureValue：租户不存在且无版本配置时返回 null")
    void getFeatureValueReturnsNullWhenTenantMissing() {
        when(featureValueRepository.getFeatureValueByTenant(TENANT_ID, "f1")).thenReturn(null);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThat(service.getFeatureValue(TENANT_ID, "f1")).isNull();
    }

    @Test
    @DisplayName("getFeatureValue：版本也没有配置时返回 null")
    void getFeatureValueReturnsNullWhenEditionValueMissing() {
        when(featureValueRepository.getFeatureValueByTenant(TENANT_ID, "f1")).thenReturn(null);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(EDITION_ID)));
        when(featureValueRepository.getFeatureValueByEdition(EDITION_ID, "f1")).thenReturn(null);

        assertThat(service.getFeatureValue(TENANT_ID, "f1")).isNull();
    }
}
