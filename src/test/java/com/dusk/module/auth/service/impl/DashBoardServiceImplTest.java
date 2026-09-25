package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.auth.dto.RoleSimpleDto;
import com.dusk.common.rpc.auth.dto.UserFullListDto;
import com.dusk.common.rpc.auth.dto.UserRoleDto;
import com.dusk.common.rpc.auth.dto.role.RoleListDto;
import com.dusk.common.rpc.auth.service.IRoleRpcService;
import com.dusk.common.rpc.auth.service.IUserRpcService;
import com.dusk.module.auth.dto.dashboard.ClassifyDetailDto;
import com.dusk.module.auth.dto.dashboard.CreateOrUpdateClassify;
import com.dusk.module.auth.dto.dashboard.CreateOrUpdateDashBoardPermission;
import com.dusk.module.auth.dto.dashboard.CreateOrUpdateTheme;
import com.dusk.module.auth.dto.dashboard.CreateOrUpdateZone;
import com.dusk.module.auth.dto.dashboard.CreateOrUpdateZoneItemRef;
import com.dusk.module.auth.dto.dashboard.GetThemeInput;
import com.dusk.module.auth.dto.dashboard.RemoveDashBoardPermission;
import com.dusk.module.auth.dto.dashboard.ThemeDetailDto;
import com.dusk.module.auth.dto.dashboard.ThemeListDto;
import com.dusk.module.auth.dto.dashboard.UserMainDashBoardDto;
import com.dusk.module.auth.entity.dashboard.DashboardClassify;
import com.dusk.module.auth.entity.dashboard.DashboardModule;
import com.dusk.module.auth.entity.dashboard.DashboardModuleItem;
import com.dusk.module.auth.entity.dashboard.DashboardPermission;
import com.dusk.module.auth.entity.dashboard.DashboardTheme;
import com.dusk.module.auth.entity.dashboard.DashboardZone;
import com.dusk.module.auth.entity.dashboard.DashboardZoneItemRef;
import com.dusk.module.auth.repository.dashboard.IDashBoardClassifyRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardModuleItemRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardModuleRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardPermissionRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardThemeRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardZoneItemRefRepository;
import com.dusk.module.auth.repository.dashboard.IDashBoardZoneRepository;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DashBoardServiceImpl} 单元测试，目标：覆盖 service.impl 门禁包的数据大屏主题/栏目/区域逻辑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashBoardServiceImplTest {

    @Mock
    private IDashBoardThemeRepository themeRepository;
    @Mock
    private IDashBoardClassifyRepository classifyRepository;
    @Mock
    private IDashBoardModuleRepository moduleRepository;
    @Mock
    private IDashBoardModuleItemRepository moduleItemRepository;
    @Mock
    private IDashBoardPermissionRepository permissionRepository;
    @Mock
    private IDashBoardZoneRepository zoneRepository;
    @Mock
    private IDashBoardZoneItemRefRepository zoneItemRefRepository;
    @Mock
    private IUserRpcService userRpcService;
    @Mock
    private IRoleRpcService roleRpcService;
    @Mock
    private JPAQueryFactory queryFactory;

    private DashBoardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new DashBoardServiceImpl());
        ReflectionTestUtils.setField(service, "repository", themeRepository);
        ReflectionTestUtils.setField(service, "themeRepository", themeRepository);
        ReflectionTestUtils.setField(service, "classifyRepository", classifyRepository);
        ReflectionTestUtils.setField(service, "moduleRepository", moduleRepository);
        ReflectionTestUtils.setField(service, "moduleItemRepository", moduleItemRepository);
        ReflectionTestUtils.setField(service, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(service, "zoneRepository", zoneRepository);
        ReflectionTestUtils.setField(service, "zoneItemRefRepository", zoneItemRefRepository);
        ReflectionTestUtils.setField(service, "userRpcService", userRpcService);
        ReflectionTestUtils.setField(service, "roleRpcService", roleRpcService);
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static DashboardTheme theme(Long id, String name, boolean mainPage) {
        DashboardTheme t = new DashboardTheme();
        t.setId(id);
        t.setName(name);
        t.setTitle("t" + id);
        t.setThemeType("dark");
        t.setMainPage(mainPage);
        return t;
    }

    private static DashboardClassify classify(Long id, Long themeId, int seq) {
        DashboardClassify c = new DashboardClassify();
        c.setId(id);
        c.setName("c" + id);
        c.setThemeId(themeId);
        c.setSeq(seq);
        c.setZoneNum(1);
        return c;
    }

    private static DashboardZone zone(Long id, Long classifyId) {
        DashboardZone z = new DashboardZone();
        z.setId(id);
        z.setName("z" + id);
        z.setClassifyId(classifyId);
        z.setZonePosition(1);
        return z;
    }

    private static UserRoleDto userRole(Long id) {
        UserRoleDto dto = new UserRoleDto();
        dto.setId(id);
        return dto;
    }

    // ------------------------------------------------------------------
    // saveTheme
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saveTheme：名称重复且非自身时抛业务异常")
    void saveTheme_duplicate() {
        CreateOrUpdateTheme input = new CreateOrUpdateTheme();
        input.setName("th");
        input.setId(2L);
        when(themeRepository.findAllByName("th")).thenReturn(List.of(theme(1L, "th", false)));

        assertThatThrownBy(() -> service.saveTheme(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在名称");
    }

    @Test
    @DisplayName("saveTheme：名称唯一时创建或更新")
    void saveTheme_success() {
        CreateOrUpdateTheme input = new CreateOrUpdateTheme();
        input.setName("th");
        when(themeRepository.findAllByName("th")).thenReturn(List.of());
        DashboardTheme created = theme(1L, "th", false);
        doReturn(created).when(service).createOrUpdate(any(), any(), any(Class.class));

        assertThat(service.saveTheme(input)).isSameAs(created);
    }

    // ------------------------------------------------------------------
    // getThemeList / themeDetail
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getThemeList：无条件分页并挂载栏目")
    void getThemeList_noFilter() {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        ThemeListDto dto = new ThemeListDto();
        dto.setId(5L);
        doReturn(new PageImpl<>(List.of(dto))).when(service).page(any(), any());
        when(classifyRepository.findAllByThemeIdIn(List.of(5L)))
                .thenReturn(List.of(classify(1L, 5L, 1)));

        PagedResultDto<ThemeListDto> result = service.getThemeList(new GetThemeInput());

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getClassifies()).hasSize(1);
    }

    @Test
    @DisplayName("getThemeList：按名称/标题/样式过滤且无栏目时分类为 null")
    void getThemeList_withFilters() {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        doReturn(new PageImpl<>(List.of())).when(service).page(any(), any());
        when(classifyRepository.findAllByThemeIdIn(any())).thenReturn(List.of());
        GetThemeInput input = new GetThemeInput();
        input.setName("a");
        input.setTitle("b");
        input.setThemeType("c");

        assertThat(service.getThemeList(input).getItems()).isEmpty();
    }

    @Test
    @DisplayName("getThemeList：栏目主题 id 不匹配时保留 null")
    void getThemeList_classifyNotMatched() {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        ThemeListDto dto = new ThemeListDto();
        dto.setId(5L);
        doReturn(new PageImpl<>(List.of(dto))).when(service).page(any(), any());
        when(classifyRepository.findAllByThemeIdIn(List.of(5L)))
                .thenReturn(List.of(classify(1L, 99L, 1)));

        assertThat(service.getThemeList(new GetThemeInput()).getItems().getFirst().getClassifies()).isNull();
    }

    @Test
    @DisplayName("themeDetail：返回主题详情并挂载排序后的栏目")
    void themeDetail() {
        doReturn(theme(1L, "th", true)).when(service).findT(1L);
        when(classifyRepository.findAllByThemeId(1L))
                .thenReturn(List.of(classify(2L, 1L, 2), classify(1L, 1L, 1)));
        when(zoneRepository.findAllByClassifyIdOrderByZonePosition(anyLong())).thenReturn(List.of());

        ThemeDetailDto detail = service.themeDetail(1L);

        assertThat(detail).isNotNull();
        assertThat(detail.getClassifyList()).hasSize(2);
        assertThat(detail.getClassifyList().getFirst().getId()).isEqualTo(1L);
    }

    // ------------------------------------------------------------------
    // saveClassify / classifyDetail
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saveClassify：新增栏目并重建区域与统计项")
    void saveClassify_add() {
        when(classifyRepository.save(any(DashboardClassify.class))).thenAnswer(inv -> {
            DashboardClassify c = inv.getArgument(0);
            c.setId(11L);
            return c;
        });
        when(zoneRepository.save(any(DashboardZone.class))).thenAnswer(inv -> {
            DashboardZone z = inv.getArgument(0);
            z.setId(21L);
            return z;
        });
        CreateOrUpdateClassify input = new CreateOrUpdateClassify();
        input.setName("c");
        input.setThemeId(1L);
        CreateOrUpdateZone zone = new CreateOrUpdateZone();
        zone.setId(7L);
        zone.setName("z");
        CreateOrUpdateZoneItemRef ref = new CreateOrUpdateZoneItemRef();
        ref.setModuleId(1L);
        ref.setModuleItemId(2L);
        zone.setZoneItems(new ArrayList<>(List.of(ref)));
        input.setZones(new ArrayList<>(List.of(zone)));

        DashboardClassify result = service.saveClassify(input);

        assertThat(result.getId()).isEqualTo(11L);
        verify(zoneRepository).deleteAllByClassifyId(11L);
        verify(zoneItemRefRepository).deleteAllByZoneId(7L);
        verify(zoneItemRefRepository).save(any(DashboardZoneItemRef.class));
    }

    @Test
    @DisplayName("saveClassify：区域无统计项时不写入关联")
    void saveClassify_updateNoZoneItems() {
        when(classifyRepository.findById(9L)).thenReturn(Optional.of(classify(9L, 1L, 1)));
        when(classifyRepository.save(any(DashboardClassify.class))).thenAnswer(inv -> inv.getArgument(0));
        when(zoneRepository.save(any(DashboardZone.class))).thenAnswer(inv -> {
            DashboardZone z = inv.getArgument(0);
            z.setId(21L);
            return z;
        });
        CreateOrUpdateClassify input = new CreateOrUpdateClassify();
        input.setId(9L);
        input.setName("c");
        input.setZoneNum(1);
        CreateOrUpdateZone zone = new CreateOrUpdateZone();
        zone.setId(7L);
        zone.setZoneItems(null);
        input.setZones(new ArrayList<>(List.of(zone)));

        service.saveClassify(input);

        verify(zoneItemRefRepository, never()).save(any(DashboardZoneItemRef.class));
    }

    @Test
    @DisplayName("saveClassify：栏目记录不存在抛业务异常")
    void saveClassify_notFound() {
        when(classifyRepository.findById(9L)).thenReturn(Optional.empty());
        CreateOrUpdateClassify input = new CreateOrUpdateClassify();
        input.setId(9L);
        input.setZones(new ArrayList<>());

        assertThatThrownBy(() -> service.saveClassify(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("栏目记录");
    }

    @Test
    @DisplayName("classifyDetail：返回栏目详情及区域统计项")
    void classifyDetail() {
        when(classifyRepository.findById(1L)).thenReturn(Optional.of(classify(1L, 1L, 1)));
        when(zoneRepository.findAllByClassifyIdOrderByZonePosition(1L))
                .thenReturn(List.of(zone(5L, 1L)));
        DashboardZoneItemRef ref = new DashboardZoneItemRef();
        ref.setId(6L);
        ref.setModuleId(7L);
        ref.setModuleItemId(8L);
        when(zoneItemRefRepository.findAllByZoneId(5L)).thenReturn(List.of(ref));
        when(moduleRepository.findById(7L)).thenReturn(Optional.of(new DashboardModule()));
        when(moduleItemRepository.findById(8L)).thenReturn(Optional.of(new DashboardModuleItem()));

        ClassifyDetailDto detail = service.classifyDetail(1L);

        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getZones()).hasSize(1);
    }

    @Test
    @DisplayName("classifyDetail：模块与统计项缺失时回填 null")
    void classifyDetail_missingRefs() {
        when(classifyRepository.findById(1L)).thenReturn(Optional.of(classify(1L, 1L, 1)));
        when(zoneRepository.findAllByClassifyIdOrderByZonePosition(1L)).thenReturn(List.of(zone(5L, 1L)));
        DashboardZoneItemRef ref = new DashboardZoneItemRef();
        ref.setId(6L);
        ref.setModuleId(7L);
        ref.setModuleItemId(8L);
        when(zoneItemRefRepository.findAllByZoneId(5L)).thenReturn(List.of(ref));
        when(moduleRepository.findById(7L)).thenReturn(Optional.empty());
        when(moduleItemRepository.findById(8L)).thenReturn(Optional.empty());

        assertThat(service.classifyDetail(1L).getZones()).hasSize(1);
    }

    @Test
    @DisplayName("classifyDetail：栏目不存在抛业务异常")
    void classifyDetail_notFound() {
        when(classifyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.classifyDetail(1L)).isInstanceOf(BusinessException.class);
    }

    // ------------------------------------------------------------------
    // 删除
    // ------------------------------------------------------------------

    @Test
    @DisplayName("removeClassify：先删区域关联再删栏目")
    void removeClassify() {
        DashboardClassify c = classify(3L, 1L, 1);
        when(classifyRepository.findById(3L)).thenReturn(Optional.of(c));
        when(zoneRepository.findAllByClassifyIdOrderByZonePosition(3L)).thenReturn(List.of(zone(4L, 3L)));

        service.removeClassify(3L);

        verify(zoneItemRefRepository).deleteAllByZoneIdIn(List.of(4L));
        verify(classifyRepository).delete(c);
    }

    @Test
    @DisplayName("removeClassify：栏目不存在抛业务异常")
    void removeClassify_notFound() {
        when(classifyRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeClassify(3L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("removeTheme：级联清理栏目、区域与关联")
    void removeTheme() {
        when(classifyRepository.findAllByThemeId(1L)).thenReturn(List.of(classify(2L, 1L, 1)));
        when(zoneRepository.findAllByClassifyIdIn(List.of(2L))).thenReturn(List.of(zone(3L, 2L)));

        service.removeTheme(1L);

        verify(zoneRepository).deleteAll(any());
        verify(zoneItemRefRepository).deleteAllByZoneIdIn(List.of(3L));
        verify(classifyRepository).deleteAllByThemeId(1L);
        verify(themeRepository).deleteById(1L);
    }

    // ------------------------------------------------------------------
    // 权限
    // ------------------------------------------------------------------

    @Test
    @DisplayName("setDashBardPermission：主题不存在抛异常")
    void setDashBardPermission_themeNotFound() {
        doReturn(Optional.empty()).when(service).findById(1L);
        CreateOrUpdateDashBoardPermission input = new CreateOrUpdateDashBoardPermission();
        input.setThemeId(1L);

        assertThatThrownBy(() -> service.setDashBardPermission(input))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("setDashBardPermission：roleIds 为空时不做写入")
    void setDashBardPermission_nullRoles() {
        doReturn(Optional.of(theme(1L, "th", false))).when(service).findById(1L);
        CreateOrUpdateDashBoardPermission input = new CreateOrUpdateDashBoardPermission();
        input.setThemeId(1L);
        input.setRoleIds(null);

        service.setDashBardPermission(input);

        verify(permissionRepository, never()).save(any(DashboardPermission.class));
    }

    @Test
    @DisplayName("setDashBardPermission：缺权限时新增，已有权限时跳过")
    void setDashBardPermission_mixed() {
        doReturn(Optional.of(theme(1L, "th", false))).when(service).findById(1L);
        when(permissionRepository.findByThemeIdAndRoleId(1L, 2L)).thenReturn(null);
        when(permissionRepository.findByThemeIdAndRoleId(1L, 3L)).thenReturn(new DashboardPermission(1L, 3L));
        CreateOrUpdateDashBoardPermission input = new CreateOrUpdateDashBoardPermission();
        input.setThemeId(1L);
        input.setRoleIds(new ArrayList<>(List.of(2L, 3L)));

        service.setDashBardPermission(input);

        verify(permissionRepository).save(any(DashboardPermission.class));
    }

    @Test
    @DisplayName("removeDashBardPermission：主题存在时删除角色关联")
    void removeDashBardPermission() {
        doReturn(Optional.of(theme(1L, "th", false))).when(service).findById(1L);
        RemoveDashBoardPermission input = new RemoveDashBoardPermission();
        input.setThemeId(1L);
        input.setRoleId(2L);

        service.removeDashBardPermission(input);

        verify(permissionRepository).deleteByThemeIdAndRoleId(1L, 2L);
    }

    // ------------------------------------------------------------------
    // 主题授权查询
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getDashBoardThemeByUserId：用户不存在抛异常")
    void getDashBoardThemeByUserId_userNotFound() {
        when(userRpcService.getUserFullById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.getDashBoardThemeByUserId(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("getDashBoardThemeByUserId：用户无角色返回空列表")
    void getDashBoardThemeByUserId_noRoles() {
        UserFullListDto user = new UserFullListDto();
        user.setUserRoles(new ArrayList<>());
        when(userRpcService.getUserFullById(1L)).thenReturn(user);

        assertThat(service.getDashBoardThemeByUserId(1L)).isEmpty();
    }

    @Test
    @DisplayName("getDashBoardThemeByUserId：按角色权限返回主题列表")
    void getDashBoardThemeByUserId_success() {
        UserFullListDto user = new UserFullListDto();
        user.setUserRoles(new ArrayList<>(List.of(userRole(7L))));
        when(userRpcService.getUserFullById(1L)).thenReturn(user);
        when(permissionRepository.findAllByRoleIdIn(List.of(7L)))
                .thenReturn(List.of(new DashboardPermission(5L, 7L)));
        when(themeRepository.findAllByIdIn(List.of(5L))).thenReturn(List.of(theme(5L, "th", false)));

        assertThat(service.getDashBoardThemeByUserId(1L)).hasSize(1);
    }

    @Test
    @DisplayName("getDashBoardThemeUser：主题不存在抛异常")
    void getDashBoardThemeUser_themeNotFound() {
        doReturn(Optional.empty()).when(service).findById(1L);

        assertThatThrownBy(() -> service.getDashBoardThemeUser(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getDashBoardThemeUser：无权限记录返回空列表")
    void getDashBoardThemeUser_noPermissions() {
        doReturn(Optional.of(theme(1L, "th", false))).when(service).findById(1L);
        when(permissionRepository.findAllByThemeId(1L)).thenReturn(List.of());

        assertThat(service.getDashBoardThemeUser(1L)).isEmpty();
    }

    @Test
    @DisplayName("getDashBoardThemeUser：权限角色 id 全空返回空列表")
    void getDashBoardThemeUser_noRoleIds() {
        doReturn(Optional.of(theme(1L, "th", false))).when(service).findById(1L);
        DashboardPermission p = new DashboardPermission();
        p.setThemeId(1L);
        p.setRoleId(null);
        when(permissionRepository.findAllByThemeId(1L)).thenReturn(List.of(p));

        assertThat(service.getDashBoardThemeUser(1L)).isEmpty();
    }

    @Test
    @DisplayName("getDashBoardThemeUser：返回角色精简列表")
    void getDashBoardThemeUser_success() {
        doReturn(Optional.of(theme(1L, "th", false))).when(service).findById(1L);
        when(permissionRepository.findAllByThemeId(1L))
                .thenReturn(List.of(new DashboardPermission(1L, 7L)));
        when(roleRpcService.getRolesByIds(List.of(7L))).thenReturn(List.of(new RoleListDto()));

        List<RoleSimpleDto> result = service.getDashBoardThemeUser(1L);

        assertThat(result).hasSize(1);
    }

    // ------------------------------------------------------------------
    // 主大屏
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getUserMainDashBoard：无主大屏时返回无权限")
    void getUserMainDashBoard_noMainTheme() {
        when(themeRepository.findFirstByMainPage(true)).thenReturn(null);

        UserMainDashBoardDto result = service.getUserMainDashBoard(1L);

        assertThat(result.getMainDashBoardId()).isNull();
        assertThat(result.getPermission()).isFalse();
    }

    @Test
    @DisplayName("getUserMainDashBoard：用户不存在抛异常")
    void getUserMainDashBoard_userNotFound() {
        when(themeRepository.findFirstByMainPage(true)).thenReturn(theme(1L, "th", true));
        when(userRpcService.getUserFullById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.getUserMainDashBoard(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getUserMainDashBoard：用户无角色时返回无权限")
    void getUserMainDashBoard_noRoles() {
        when(themeRepository.findFirstByMainPage(true)).thenReturn(theme(1L, "th", true));
        UserFullListDto user = new UserFullListDto();
        user.setUserRoles(new ArrayList<>());
        when(userRpcService.getUserFullById(1L)).thenReturn(user);

        assertThat(service.getUserMainDashBoard(1L).getPermission()).isFalse();
    }

    @Test
    @DisplayName("getUserMainDashBoard：命中权限时返回主大屏 id 且有权限")
    void getUserMainDashBoard_withPermission() {
        when(themeRepository.findFirstByMainPage(true)).thenReturn(theme(1L, "th", true));
        UserFullListDto user = new UserFullListDto();
        user.setUserRoles(new ArrayList<>(List.of(userRole(7L))));
        when(userRpcService.getUserFullById(1L)).thenReturn(user);
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.selectFrom(any(EntityPath.class))).thenReturn(query);
        when(query.fetchCount()).thenReturn(1L);

        UserMainDashBoardDto result = service.getUserMainDashBoard(1L);

        assertThat(result.getMainDashBoardId()).isEqualTo(1L);
        assertThat(result.getPermission()).isTrue();
    }

    @Test
    @DisplayName("checkMainDashBoard：无主大屏返回 true，有主大屏时比对 id")
    void checkMainDashBoard() {
        when(themeRepository.findFirstByMainPage(true)).thenReturn(null);
        assertThat(service.checkMainDashBoard(1L)).isTrue();

        when(themeRepository.findFirstByMainPage(true)).thenReturn(theme(1L, "th", true));
        assertThat(service.checkMainDashBoard(1L)).isTrue();
        assertThat(service.checkMainDashBoard(2L)).isFalse();
    }

    @Test
    @DisplayName("getThemeList：page 查询中的 where 谓词不参与断言的兜底校验")
    void getThemeList_whereStubbed() {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        doReturn(new PageImpl<>(List.of())).when(service).page(any(), any());
        GetThemeInput input = new GetThemeInput();
        input.setName("x");

        assertThat(service.getThemeList(input).getItems()).isEmpty();
        verify(query).where(any(Predicate.class));
    }
}
