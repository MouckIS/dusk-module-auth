package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.permission.MultiTenancySides;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.edition.EditionEditDto;
import com.dusk.module.auth.dto.edition.GetEditionInput;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.repository.IFeatureValueRepository;
import com.dusk.module.auth.repository.ISubscribableEditionRepository;
import com.dusk.module.auth.repository.ITenantPermissionRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import com.dusk.module.auth.service.IFeatureService;
import com.dusk.module.auth.service.ITenantPermissionService;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SubscribableEditionServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscribableEditionServiceImplTest {

    @Mock
    private ITenantRepository tenantRepository;
    @Mock
    private IFeatureValueRepository featureValueRepository;
    @Mock
    private ITenantPermissionRepository tenantPermissionRepository;
    @Mock
    private IAuthPermissionManager authPermissionManager;
    @Mock
    private IFeatureService featureService;
    @Mock
    private ITenantPermissionService tenantPermissionService;
    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private ISubscribableEditionRepository repository;

    private SubscribableEditionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscribableEditionServiceImpl();
        ReflectionTestUtils.setField(service, "tenantRepository", tenantRepository);
        ReflectionTestUtils.setField(service, "featureValueRepository", featureValueRepository);
        ReflectionTestUtils.setField(service, "tenantPermissionRepository", tenantPermissionRepository);
        ReflectionTestUtils.setField(service, "authPermissionManager", authPermissionManager);
        ReflectionTestUtils.setField(service, "featureService", featureService);
        ReflectionTestUtils.setField(service, "tenantPermissionService", tenantPermissionService);
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    private static EditionEditDto editDto(Long id, String displayName) {
        EditionEditDto dto = new EditionEditDto();
        dto.setId(id);
        dto.setDisplayName(displayName);
        // updateEdition 使用 Spring BeanUtils.copyProperties 将 DTO 复制到实体，
        // version 字段目标类型为原始 int，为 null 会抛 FatalBeanException，故必须显式赋值。
        dto.setVersion(0);
        return dto;
    }

    private static SubscribableEdition edition(Long id, String displayName, BigDecimal monthly, BigDecimal annual) {
        SubscribableEdition entity = new SubscribableEdition();
        entity.setId(id);
        entity.setDisplayName(displayName);
        entity.setMonthlyPrice(monthly);
        entity.setAnnualPrice(annual);
        return entity;
    }

    private static SubscribableEdition freeEdition(Long id) {
        return edition(id, "免费版", BigDecimal.ZERO, BigDecimal.ZERO);
    }

    // ---------------- createEdition ----------------

    @Test
    @DisplayName("createEdition：名称唯一且无到期版本时直接落库")
    void createEditionSucceeds() {
        when(repository.findByDisplayName("专业版")).thenReturn(Optional.empty());

        SubscribableEdition created = service.createEdition(editDto(null, "专业版"));

        assertThat(created.getDisplayName()).isEqualTo("专业版");
        verify(repository).save(created);
    }

    @Test
    @DisplayName("createEdition：名称重复时抛出业务异常")
    void createEditionThrowsOnDuplicateName() {
        when(repository.findByDisplayName("专业版"))
                .thenReturn(Optional.of(edition(9L, "专业版", null, null)));

        assertThatThrownBy(() -> service.createEdition(editDto(null, "专业版")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在，请重新输入");
    }

    @Test
    @DisplayName("createEdition：到期版本为免费版时允许创建")
    void createEditionWithFreeExpiringEdition() {
        when(repository.findByDisplayName("专业版")).thenReturn(Optional.empty());
        when(repository.findById(5L)).thenReturn(Optional.of(freeEdition(5L)));

        EditionEditDto dto = editDto(null, "专业版");
        dto.setExpiringEditionId("5");

        assertThat(service.createEdition(dto).getExpiringEditionId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("createEdition：到期版本不存在时抛出业务异常")
    void createEditionThrowsWhenExpiringEditionMissing() {
        when(repository.findByDisplayName("专业版")).thenReturn(Optional.empty());
        when(repository.findById(5L)).thenReturn(Optional.empty());

        EditionEditDto dto = editDto(null, "专业版");
        dto.setExpiringEditionId("5");

        assertThatThrownBy(() -> service.createEdition(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未找到相应的版本信息");
    }

    @Test
    @DisplayName("createEdition：到期版本为付费版时抛出业务异常")
    void createEditionThrowsWhenExpiringEditionNotFree() {
        when(repository.findByDisplayName("专业版")).thenReturn(Optional.empty());
        when(repository.findById(5L))
                .thenReturn(Optional.of(edition(5L, "付费版", BigDecimal.TEN, BigDecimal.TEN)));

        EditionEditDto dto = editDto(null, "专业版");
        dto.setExpiringEditionId("5");

        assertThatThrownBy(() -> service.createEdition(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("到期后强制转为免费版");
    }

    // ---------------- updateEdition ----------------

    @Test
    @DisplayName("updateEdition：id 为空时不做任何处理")
    void updateEditionSkipsWhenIdNull() {
        service.updateEdition(editDto(null, "任意"));

        verify(repository, never()).findById(anyLong());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updateEdition：名称唯一时复制属性并落库")
    void updateEditionSucceeds() {
        SubscribableEdition existing = edition(1L, "旧名", BigDecimal.TEN, BigDecimal.TEN);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByDisplayName("新名")).thenReturn(Optional.empty());

        service.updateEdition(editDto(1L, "新名"));

        assertThat(existing.getDisplayName()).isEqualTo("新名");
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("updateEdition：目标版本不存在时抛出业务异常")
    void updateEditionThrowsWhenMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEdition(editDto(1L, "新名")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未找到相应的版本信息");
    }

    @Test
    @DisplayName("updateEdition：改名后与他人重名时抛出业务异常")
    void updateEditionThrowsOnDuplicateName() {
        when(repository.findById(1L)).thenReturn(Optional.of(edition(1L, "旧名", null, null)));
        when(repository.findByDisplayName("占用名"))
                .thenReturn(Optional.of(edition(2L, "占用名", null, null)));

        assertThatThrownBy(() -> service.updateEdition(editDto(1L, "占用名")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("updateEdition：名称未变化时不触发重名查询")
    void updateEditionSkipsUniquenessCheckWhenNameUnchanged() {
        SubscribableEdition existing = edition(1L, "同名", null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByDisplayName("同名")).thenReturn(Optional.of(existing));

        service.updateEdition(editDto(1L, "同名"));

        verify(repository).save(existing);
    }

    @Test
    @DisplayName("updateEdition：免费版改为付费版且被其他版本引用时抛出业务异常")
    void updateEditionThrowsWhenFreeEditionReferenced() {
        SubscribableEdition existing = freeEdition(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByDisplayName("同名")).thenReturn(Optional.of(existing));
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(existing));

        EditionEditDto dto = editDto(1L, "同名");
        dto.setMonthlyPrice(BigDecimal.TEN);
        dto.setAnnualPrice(BigDecimal.TEN);

        assertThatThrownBy(() -> service.updateEdition(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("此版本用作其他版本订阅到期后版本");
    }

    @Test
    @DisplayName("updateEdition：免费版改为付费版但无人引用时允许更新")
    void updateEditionAllowsFreeToPaidWhenUnreferenced() {
        SubscribableEdition existing = freeEdition(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByDisplayName("同名")).thenReturn(Optional.of(existing));
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        EditionEditDto dto = editDto(1L, "同名");
        dto.setMonthlyPrice(BigDecimal.TEN);

        service.updateEdition(dto);

        verify(repository).save(existing);
    }

    // ---------------- 查询 ----------------

    @Test
    @DisplayName("getEditions：filter 为空时不追加模糊条件")
    void getEditionsWithoutFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        GetEditionInput input = new GetEditionInput();
        input.setFilter("   ");

        assertThat(service.getEditions(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("getEditions：filter 非空时按名称模糊查询")
    void getEditionsWithFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        GetEditionInput input = new GetEditionInput();
        input.setFilter("专业");

        assertThat(service.getEditions(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("findByDisplayName：透传仓储查询结果")
    void findByDisplayNameDelegates() {
        SubscribableEdition entity = freeEdition(1L);
        when(repository.findByDisplayName("免费版")).thenReturn(Optional.of(entity));

        assertThat(service.findByDisplayName("免费版")).contains(entity);
    }

    // ---------------- deleteEdition ----------------

    @Test
    @DisplayName("deleteEdition：版本不存在时抛出业务异常")
    void deleteEditionThrowsWhenMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEdition(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("数据不存在");
    }

    @Test
    @DisplayName("deleteEdition：仍有租户关联时抛出业务异常")
    void deleteEditionThrowsWhenTenantBound() {
        when(repository.findById(1L)).thenReturn(Optional.of(freeEdition(1L)));
        when(tenantRepository.count(any(Specification.class))).thenReturn(3L);

        assertThatThrownBy(() -> service.deleteEdition(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除");
    }

    @Test
    @DisplayName("deleteEdition：无租户关联时删除版本并清理特性值/权限")
    void deleteEditionCleansUp() {
        when(repository.findById(1L)).thenReturn(Optional.of(freeEdition(1L)));
        when(tenantRepository.count(any(Specification.class))).thenReturn(0L);

        service.deleteEdition(1L);

        verify(repository).deleteById(1L);
        verify(featureValueRepository).deleteByEditionId(1L);
        verify(tenantPermissionRepository).deleteByEditionId(1L);
    }

    // ---------------- export ----------------

    @Test
    @DisplayName("export：版本不存在时抛出业务异常")
    void exportThrowsWhenMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.export(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("版本不存在或已被删除");
    }

    // ---------------- 内部权限递归 ----------------

    @Test
    @DisplayName("recursionAddGrantedPermission：命中节点时加入集合并向上递归到父节点")
    void recursionAddsMatchedAndParent() {
        Permission parent = new Permission("Pages.Administration", "管理", MultiTenancySides.All);
        Permission child = parent.createChildPermission("Pages.Role", "角色");

        Set<String> granted = new LinkedHashSet<>();
        service.recursionAddGrantedPermission("Pages.Role", List.of(parent, child), granted);

        assertThat(granted).containsExactly("Pages.Role", "Pages.Administration");
    }

    @Test
    @DisplayName("recursionAddGrantedPermission：未命中任何节点时不写入集合")
    void recursionSkipsWhenNoMatch() {
        Permission root = new Permission("Pages.Administration", "管理", MultiTenancySides.All);

        Set<String> granted = new LinkedHashSet<>();
        service.recursionAddGrantedPermission("Pages.Unknown", List.of(root), granted);

        assertThat(granted).isEmpty();
    }

    @Test
    @DisplayName("recursionAddGrantedPermission：父节点名为空时停止向上递归")
    void recursionStopsWhenParentNameBlank() {
        Permission blankParent = new Permission("", "", MultiTenancySides.All);
        Permission child = new Permission("Pages.Role", "角色", MultiTenancySides.All);
        child.setParent(blankParent);

        Set<String> granted = new LinkedHashSet<>();
        service.recursionAddGrantedPermission("Pages.Role", List.of(child), granted);

        assertThat(granted).containsExactly("Pages.Role");
    }

    @Test
    @DisplayName("findEditionPermission：按版本 id 查询已授权权限名集合")
    void findEditionPermissionQueries() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.fetch()).thenReturn(new ArrayList<String>(List.of("Pages.Role")));

        assertThat(service.findEditionPermission(1L)).containsExactly("Pages.Role");
        verify(queryFactory).select(any(Expression.class));
    }

    @Test
    @DisplayName("deleteEdition：租户数为 0 时不抛出异常且清理动作按序执行")
    void deleteEditionRunsCleanupInOrder() {
        when(repository.findById(2L)).thenReturn(Optional.of(freeEdition(2L)));
        when(tenantRepository.count(any(Specification.class))).thenReturn(0L);

        service.deleteEdition(2L);

        verify(repository).deleteById(2L);
    }

    @Test
    @DisplayName("createEdition：Mapper 将字符串到期版本号解析为 Long")
    void createEditionMapsExpiringEditionId() {
        when(repository.findByDisplayName(anyString())).thenReturn(Optional.empty());
        when(repository.findById(7L)).thenReturn(Optional.of(freeEdition(7L)));

        EditionEditDto dto = editDto(null, "企业版");
        dto.setExpiringEditionId("7");

        assertThat(service.createEdition(dto).getExpiringEditionId()).isEqualTo(7L);
        assertThat(Tenant.Fields.edition).isNotBlank();
    }
}
