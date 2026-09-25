package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.ListResultDto;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitDto;
import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.common.rpc.auth.enums.EnumResetType;
import com.dusk.module.auth.dto.orga.CreateOrganizationUnitInput;
import com.dusk.module.auth.dto.orga.GetOrganizationUnitUsersExtInput;
import com.dusk.module.auth.dto.orga.GetOrganizationUnitUsersForSelectInput;
import com.dusk.module.auth.dto.orga.MoveOrganizationUnitInput;
import com.dusk.module.auth.dto.orga.OrganizationStationUnitDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserInfoListDto;
import com.dusk.module.auth.dto.orga.UpdateOrganizationUnitInput;
import com.dusk.module.auth.dto.orga.UserToOrganizationUnitInput;
import com.dusk.module.auth.dto.orga.UsersToOrganizationUnitInput;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.entity.OrganizationManager;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IOrganizationManagerRepository;
import com.dusk.module.auth.repository.IOrganizationUnitRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ISerialNoService;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OrganizationUnitServiceImpl} 单元测试，目标：补齐 service.impl 门禁包的
 * 行/分支覆盖（该类为门禁包内单体体量最大、基线零覆盖的类）。
 *
 * <p>说明：
 * <ul>
 *   <li>{@code importUnitByExcel} 的 Excel 解析主链路依赖 EasyExcel 与真实模板，
 *       仅覆盖 {@code IOException → BusinessException} 的异常分支。</li>
 *   <li>{@code getSerialNos(int)} 依赖 {@code BaseService#getEntityClass()} 读取
 *       {@code getClass().getGenericSuperclass()}，Mockito spy 的子类会让泛型超类
 *       退化为原始类型而抛 {@code ClassCastException}，故该用例使用真实实例驱动。</li>
 *   <li>{@code Specifications.where(...)} 内部 lambda 仅在仓储真正执行规格时求值，
 *       本测试统一桩化 {@code findAll(Specification)}，故 lambda 体内语句不计入覆盖。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationUnitServiceImplTest {

    @Mock
    private IOrganizationUnitRepository repository;
    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private ISerialNoService serialNoService;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private IOrganizationManagerRepository organizationManagerRepository;

    private OrganizationUnitServiceImpl service;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.spy(new OrganizationUnitServiceImpl());
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "queryFactory", queryFactory);
        ReflectionTestUtils.setField(service, "serialNoService", serialNoService);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "organizationManagerRepository", organizationManagerRepository);
    }

    @AfterEach
    void tearDown() {
        LoginUserIdContextHolder.clear();
        DataFilterContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static OrganizationUnit unit(Long id, Long parentId, String path, String displayName,
                                        boolean station, boolean stationEnabled) {
        OrganizationUnit u = new OrganizationUnit();
        u.setId(id);
        u.setParentId(parentId);
        u.setPath(path);
        u.setDisplayName(displayName);
        u.setStation(station);
        u.setStationEnabled(stationEnabled);
        u.setSortIndex(1);
        u.setType(EUnitType.Inner);
        u.setUsers(new ArrayList<>());
        return u;
    }

    private static User user(Long id, EUnitType type) {
        User u = new User();
        u.setId(id);
        u.setName("u" + id);
        u.setUserName("user" + id);
        u.setUserType(type);
        u.setUserStatus(UserStatus.OnJob);
        u.setActive(true);
        u.setUserRoles(new ArrayList<>());
        return u;
    }

    private static GetOrganizationUnitUsersInput usersInput(List<Long> ids, boolean deep) {
        GetOrganizationUnitUsersInput input = new GetOrganizationUnitUsersInput();
        input.setOrganizationUnitIds(ids == null ? new ArrayList<>() : new ArrayList<>(ids));
        input.setDeepQuery(deep);
        return input;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubFindAllSpec(List<OrganizationUnit> result) {
        doReturn(result).when(service).findAll(any(Specification.class));
    }

    // ------------------------------------------------------------------
    // getExternalOrganizationUnits
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getExternalOrganizationUnits：内部人员查全部外部机构并回填管理员")
    void getExternalOrganizationUnits_innerUser() {
        LoginUserIdContextHolder.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, EUnitType.Inner)));
        when(organizationManagerRepository.findAll()).thenReturn(List.of(new OrganizationManager(9L, 77L)));
        doReturn(List.of(unit(9L, null, "9", "org9", false, true)))
                .when(service).findAll(any(Specification.class), any(Sort.class));

        ListResultDto<OrganizationStationUnitDto> result = service.getExternalOrganizationUnits();

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getManagerId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("getExternalOrganizationUnits：外部人员取所属机构及其上级")
    void getExternalOrganizationUnits_externalUser() {
        LoginUserIdContextHolder.setUserId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, EUnitType.External)));
        when(organizationManagerRepository.findAll()).thenReturn(List.of());
        OrganizationUnit own = unit(3L, null, "3", "own", false, true);
        doReturn(List.of(own)).when(service).getOrganizationUnitsByUser(any(EntityDto.class));
        doReturn(List.of(own, unit(1L, null, "1", "root", false, true)))
                .when(service).getParentOrganizations(any(EntityDto.class));

        ListResultDto<OrganizationStationUnitDto> result = service.getExternalOrganizationUnits();

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().getFirst().getManagerId()).isNull();
    }

    // ------------------------------------------------------------------
    // getOrganizationUnitUsers / findUsers
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getOrganizationUnitUsers：非深查直接用入参 id 并回填默认排序")
    void getOrganizationUnitUsers_plainIds() {
        OrganizationUnitUserListDto dto = new OrganizationUnitUserListDto(1L, "n", "u", "e", 2L, "org");
        when(repository.getOrganizationUnitUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto)));

        Page<OrganizationUnitUserListDto> result = service.getOrganizationUnitUsers(usersInput(List.of(2L), false));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getOrganizationUnitUsers：id 为空补 -1 哨兵查询全部用户")
    void getOrganizationUnitUsers_emptyIdsAddsSentinel() {
        when(repository.getOrganizationUnitUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Page<OrganizationUnitUserListDto> result = service.getOrganizationUnitUsers(usersInput(null, false));

        assertThat(result.getContent()).isEmpty();
        verify(repository).getOrganizationUnitUsers(
                org.mockito.ArgumentMatchers.argThat(set -> set.contains(-1L)), any(), any(), any());
    }

    @Test
    @DisplayName("getOrganizationUnitUsers：深查展开所有后代 id")
    void getOrganizationUnitUsers_deep() {
        doReturn(List.of(2L, 3L)).when(service).getAllDescendantIds(2L, true);
        when(repository.getOrganizationUnitUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.getOrganizationUnitUsers(usersInput(List.of(2L), true));

        verify(repository).getOrganizationUnitUsers(
                org.mockito.ArgumentMatchers.argThat(set -> set.contains(2L) && set.contains(3L)), any(), any(), any());
    }

    @Test
    @DisplayName("findUsers：id 空补 -1 哨兵")
    void findUsers_emptyIds() {
        when(repository.findUsers(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        Page<User> result = service.findUsers(usersInput(null, false));

        assertThat(result.getContent()).isEmpty();
        verify(repository).findUsers(
                org.mockito.ArgumentMatchers.argThat(set -> set.contains(-1L)), any(), any(), any());
    }

    @Test
    @DisplayName("findUsers：传入 id 时直接透传")
    void findUsers_withIds() {
        when(repository.findUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(user(1L, EUnitType.Inner))));

        Page<User> result = service.findUsers(usersInput(List.of(3L), false));

        assertThat(result.getContent()).hasSize(1);
    }

    // ------------------------------------------------------------------
    // getOrganizationUnitUsersInfo
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getOrganizationUnitUsersInfo：条件齐全时构建 QueryDSL 查询并分页")
    void getOrganizationUnitUsersInfo_full() {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        Page<OrganizationUnitUserInfoListDto> page = new PageImpl<>(List.of(new OrganizationUnitUserInfoListDto()));
        doReturn(page).when(service).page(any(), any());

        GetOrganizationUnitUsersExtInput input = new GetOrganizationUnitUsersExtInput();
        input.setDeepQuery(false);
        input.setOrganizationUnitIds(new ArrayList<>(List.of(1L)));
        input.setFilter("abc");
        input.setType(EUnitType.Inner);
        input.setDisplayDimissionUsers(false);
        input.setSorting(null);

        assertThat(service.getOrganizationUnitUsersInfo(input)).isSameAs(page);
    }

    @Test
    @DisplayName("getOrganizationUnitUsersInfo：无 id/无过滤/含离职/自带排序时走另一分支")
    void getOrganizationUnitUsersInfo_minimal() {
        JPAQuery query = mock(JPAQuery.class, Answers.RETURNS_SELF);
        when(queryFactory.select(any(Expression.class))).thenReturn(query);
        doReturn(new PageImpl<>(List.of())).when(service).page(any(), any());

        GetOrganizationUnitUsersExtInput input = new GetOrganizationUnitUsersExtInput();
        input.setDeepQuery(false);
        input.setOrganizationUnitIds(new ArrayList<>());
        input.setFilter("   ");
        input.setType(null);
        input.setDisplayDimissionUsers(true);
        input.setSorting("name");

        assertThat(service.getOrganizationUnitUsersInfo(input).getContent()).isEmpty();
    }

    // ------------------------------------------------------------------
    // 下拉列表 / 分页重载
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getOrganizationUnitUsersForSelect：orgId 为空返回空分页")
    void getOrganizationUnitUsersForSelect_nullOrg() {
        GetOrganizationUnitUsersForSelectInput input = new GetOrganizationUnitUsersForSelectInput();
        assertThat(service.getOrganizationUnitUsersForSelect(input).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("getOrganizationUnitUsersForSelect：orgId 存在委托仓储")
    void getOrganizationUnitUsersForSelect_withOrg() {
        when(repository.getOrganizationUnitUsersForSelect(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        GetOrganizationUnitUsersForSelectInput input = new GetOrganizationUnitUsersForSelectInput();
        input.setOrgId(5L);

        assertThat(service.getOrganizationUnitUsersForSelect(input).getContent()).isEmpty();
    }

    @Test
    @DisplayName("getOrganizationUnitUsers（分页重载）：包装为 PagedResultDto 并合并组织机构 id")
    void getOrganizationUnitUsers_pagedOverload() {
        OrganizationUnitUserListDto dto = new OrganizationUnitUserListDto(1L, "n", "u", "e", 2L, "org");
        when(repository.getOrganizationUnitUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto)));

        PagedAndSortedInputDto req = new PagedAndSortedInputDto();
        PagedResultDto<OrganizationUnitUserListDto> result =
                service.getOrganizationUnitUsers(req, "f", false, 3L, 4L);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getOrganizationUnitUsers（分页重载）：不传组织机构 id 时不追加")
    void getOrganizationUnitUsers_pagedOverloadNoOrgIds() {
        when(repository.getOrganizationUnitUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResultDto<OrganizationUnitUserListDto> result =
                service.getOrganizationUnitUsers(new PagedAndSortedInputDto(), null, false);

        assertThat(result.getItems()).isEmpty();
    }

    // ------------------------------------------------------------------
    // create / update / move
    // ------------------------------------------------------------------

    @Test
    @DisplayName("create：保存机构并在存在管理员时重建管理关系")
    void create_withManager() {
        CreateOrganizationUnitInput input = new CreateOrganizationUnitInput();
        input.setDisplayName("新部门");
        input.setManagerId(9L);
        OrganizationUnit saved = unit(100L, null, "100", "新部门", false, true);
        doReturn(saved).when(service).save(any(OrganizationUnit.class));
        stubFindAllSpec(List.of());

        OrganizationUnit result = service.create(input);

        assertThat(result).isNotNull();
        verify(organizationManagerRepository).deleteByOrgId(100L);
        verify(organizationManagerRepository).save(any(OrganizationManager.class));
    }

    @Test
    @DisplayName("create：无管理员时跳过管理关系维护")
    void create_withoutManager() {
        CreateOrganizationUnitInput input = new CreateOrganizationUnitInput();
        input.setDisplayName("无主部门");
        doReturn(unit(101L, null, "101", "无主部门", false, true)).when(service).save(any(OrganizationUnit.class));
        stubFindAllSpec(List.of());

        service.create(input);

        verify(organizationManagerRepository, never()).save(any(OrganizationManager.class));
        verify(organizationManagerRepository, never()).deleteByOrgId(anyLong());
    }

    @Test
    @DisplayName("create：同级重名抛业务异常")
    void create_duplicateName() {
        CreateOrganizationUnitInput input = new CreateOrganizationUnitInput();
        input.setDisplayName("重复");
        doReturn(unit(102L, null, "102", "重复", false, true)).when(service).save(any(OrganizationUnit.class));
        stubFindAllSpec(List.of(unit(1L, null, "1", "重复", false, true),
                unit(2L, null, "2", "重复", false, true)));

        assertThatThrownBy(() -> service.create(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("相同名称");
    }

    @Test
    @DisplayName("createExternalOrganization：外部人员不允许创建根节点")
    void createExternalOrganization_externalRoot() {
        LoginUserIdContextHolder.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, EUnitType.External)));
        CreateOrganizationUnitInput input = new CreateOrganizationUnitInput();
        input.setParentId(null);

        assertThatThrownBy(() -> service.createExternalOrganization(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许创建根节点");
    }

    @Test
    @DisplayName("createExternalOrganization：带父节点时走创建流程")
    void createExternalOrganization_withParent() {
        LoginUserIdContextHolder.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, EUnitType.External)));
        CreateOrganizationUnitInput input = new CreateOrganizationUnitInput();
        input.setParentId(1L);
        input.setDisplayName("外部子机构");
        doReturn(unit(103L, 1L, "1/103", "外部子机构", false, true)).when(service).save(any(OrganizationUnit.class));
        stubFindAllSpec(List.of());

        assertThat(service.createExternalOrganization(input)).isNotNull();
    }

    @Test
    @DisplayName("update：机构存在时更新并校验重名")
    void update_found() {
        OrganizationUnit existing = unit(5L, null, "5", "旧名", false, true);
        doReturn(Optional.of(existing)).when(service).findById(5L);
        doReturn(existing).when(service).save(any(OrganizationUnit.class));
        stubFindAllSpec(List.of(existing));
        UpdateOrganizationUnitInput input = new UpdateOrganizationUnitInput();
        input.setId(5L);
        input.setDisplayName("新名");

        assertThat(service.update(input)).isSameAs(existing);
        verify(service).save(existing);
    }

    @Test
    @DisplayName("update：机构不存在抛业务异常")
    void update_notFound() {
        doReturn(Optional.empty()).when(service).findById(6L);
        UpdateOrganizationUnitInput input = new UpdateOrganizationUnitInput();
        input.setId(6L);

        assertThatThrownBy(() -> service.update(input)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("move：机构存在时保存并校验重名")
    void move_found() {
        OrganizationUnit existing = unit(7L, null, "7", "移", false, true);
        doReturn(Optional.of(existing)).when(service).findById(7L);
        doReturn(existing).when(service).save(any(OrganizationUnit.class));
        stubFindAllSpec(List.of());
        MoveOrganizationUnitInput input = new MoveOrganizationUnitInput();
        input.setId(7L);

        assertThat(service.move(input)).isSameAs(existing);
    }

    @Test
    @DisplayName("move：机构不存在抛业务异常")
    void move_notFound() {
        doReturn(Optional.empty()).when(service).findById(8L);
        MoveOrganizationUnitInput input = new MoveOrganizationUnitInput();
        input.setId(8L);

        assertThatThrownBy(() -> service.move(input)).isInstanceOf(BusinessException.class);
    }

    // ------------------------------------------------------------------
    // 删除
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleteOrgById：先删用户、再删管理层、最后删机构")
    void deleteOrgById() {
        OrganizationUnitUserListDto dto = new OrganizationUnitUserListDto(1L, "n", "u", "e", 2L, "org");
        doReturn(new PageImpl<>(List.of(dto)))
                .when(service).getOrganizationUnitUsers(any(GetOrganizationUnitUsersInput.class));
        doReturn(List.of(1L, 2L)).when(service).getAllDescendantIds(anyLong(), anyBoolean());
        JPADeleteClause clause = mock(JPADeleteClause.class);
        when(queryFactory.delete(any(EntityPath.class))).thenReturn(clause);
        when(clause.where(any(Predicate.class))).thenReturn(clause);
        when(clause.execute()).thenReturn(1L);
        doNothing().when(service).deleteById(9L);

        service.deleteOrgById(9L);

        verify(organizationManagerRepository).deleteByOrgId(9L);
        verify(service).deleteById(9L);
    }

    @Test
    @DisplayName("deleteByCodes：空入参直接返回")
    void deleteByCodes_empty() {
        service.deleteByCodes(new ArrayList<>());

        verify(service, never()).findByCodes(any());
    }

    @Test
    @DisplayName("deleteByCodes：按业务编码查询后逐个删除")
    void deleteByCodes_deletes() {
        OrganizationUnit u = unit(11L, null, "11", "A", false, true);
        doReturn(List.of(u)).when(service).findByCodes(any());
        doNothing().when(service).delete(u);

        service.deleteByCodes(List.of("A"));

        verify(service).delete(u);
    }

    // ------------------------------------------------------------------
    // 用户与机构关系
    // ------------------------------------------------------------------

    @Test
    @DisplayName("removeUserFromOrganizationUnit：从机构用户中剔除指定用户")
    void removeUserFromOrganizationUnit() {
        OrganizationUnit unit = unit(5L, null, "5", "org", false, true);
        unit.setUsers(new ArrayList<>(List.of(user(1L, EUnitType.Inner), user(2L, EUnitType.Inner))));
        doReturn(Optional.of(unit)).when(service).findById(5L);
        doReturn(unit).when(service).save(any(OrganizationUnit.class));
        UserToOrganizationUnitInput input = new UserToOrganizationUnitInput();
        input.setOrganizationUnitId(5L);
        input.setUserId(1L);

        service.removeUserFromOrganizationUnit(input);

        assertThat(unit.getUsers()).extracting(User::getId).containsExactly(2L);
    }

    @Test
    @DisplayName("removeUserFromOrganizationUnit：机构不存在抛异常")
    void removeUserFromOrganizationUnit_notFound() {
        doReturn(Optional.empty()).when(service).findById(5L);
        UserToOrganizationUnitInput input = new UserToOrganizationUnitInput();
        input.setOrganizationUnitId(5L);

        assertThatThrownBy(() -> service.removeUserFromOrganizationUnit(input))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("addUsersToOrganizationUnit：仅补充尚未归属的用户")
    void addUsersToOrganizationUnit() {
        OrganizationUnit unit = unit(5L, null, "5", "org", false, true);
        unit.setUsers(new ArrayList<>(List.of(user(1L, EUnitType.Inner))));
        doReturn(Optional.of(unit)).when(service).findById(5L);
        doReturn(unit).when(service).save(any(OrganizationUnit.class));
        UsersToOrganizationUnitInput input = new UsersToOrganizationUnitInput();
        input.setOrganizationUnitId(5L);
        input.setUserIds(new ArrayList<>(List.of(1L, 2L)));

        service.addUsersToOrganizationUnit(input);

        assertThat(unit.getUsers()).extracting(User::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("addUsersToOrganizationUnit：机构不存在抛异常")
    void addUsersToOrganizationUnit_notFound() {
        doReturn(Optional.empty()).when(service).findById(5L);
        UsersToOrganizationUnitInput input = new UsersToOrganizationUnitInput();
        input.setOrganizationUnitId(5L);

        assertThatThrownBy(() -> service.addUsersToOrganizationUnit(input))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getOrganizationUnitsByUser：委托仓储并映射")
    void getOrganizationUnitsByUser() {
        when(repository.getOrganizationUnitsByUser(1L))
                .thenReturn(List.of(unit(1L, null, "1", "org1", false, true)));

        assertThat(service.getOrganizationUnitsByUser(new EntityDto(1L))).hasSize(1);
    }

    @Test
    @DisplayName("getOrganizationUnitsByUserId：按用户 id 查询并映射")
    void getOrganizationUnitsByUserId() {
        when(repository.getOrganizationUnitsByUser(1L))
                .thenReturn(List.of(unit(1L, null, "1", "org1", false, true)));

        assertThat(service.getOrganizationUnitsByUserId(1L)).hasSize(1);
    }

    @Test
    @DisplayName("getParentOrganizations：节点存在时返回自身与全部祖先")
    void getParentOrganizations_present() {
        OrganizationUnit self = unit(5L, null, "5", "self", false, true);
        when(repository.findById(5L)).thenReturn(Optional.of(self));
        doReturn(List.of(unit(1L, null, "1", "root", false, true))).when(service).getAncestors(5L);

        assertThat(service.getParentOrganizations(new EntityDto(5L))).hasSize(2);
    }

    @Test
    @DisplayName("getParentOrganizations：节点不存在返回空列表")
    void getParentOrganizations_absent() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThat(service.getParentOrganizations(new EntityDto(5L))).isEmpty();
    }

    // ------------------------------------------------------------------
    // 厂站
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getStations：查询启用的厂站")
    void getStations() {
        when(repository.findByStationAndStationEnabled(true, true))
                .thenReturn(List.of(unit(1L, null, "1", "st", true, true)));

        assertThat(service.getStations()).hasSize(1);
    }

    @Test
    @DisplayName("getStationsByParentId：递归收集所有下级厂站")
    void getStationsByParentId() {
        when(repository.getStationsByParentId(1L))
                .thenReturn(List.of(unit(2L, 1L, "1/2", "child", true, true)));
        when(repository.getStationsByParentId(2L)).thenReturn(List.of());

        assertThat(service.getStationsByParentId(1L)).hasSize(1);
    }

    @Test
    @DisplayName("getStationsByUserId：按用户机构深查过滤启用厂站")
    void getStationsByUserId() {
        when(repository.getOrganizationUnitsByUser(1L))
                .thenReturn(List.of(unit(2L, null, "2", "org", false, true)));
        doReturn(List.of(2L, 3L)).when(service).getAllDescendantIds(anyLong(), anyBoolean());
        stubFindAllSpec(List.of(unit(3L, null, "3", "station", true, true)));

        assertThat(service.getStationsByUserId(1L)).hasSize(1);
    }

    @Test
    @DisplayName("getAllStationsByUserId：按用户机构深查收集全部厂站")
    void getAllStationsByUserId() {
        when(repository.getOrganizationUnitsByUser(1L))
                .thenReturn(List.of(unit(2L, null, "2", "org", false, true)));
        doReturn(List.of(2L)).when(service).getAllDescendantIds(anyLong(), anyBoolean());
        stubFindAllSpec(List.of(unit(2L, null, "2", "station", true, false)));

        assertThat(service.getAllStationsByUserId(1L)).hasSize(1);
    }

    @Test
    @DisplayName("getStationsForFrontByUserId：映射下拉项并标记默认厂站")
    void getStationsForFrontByUserId() {
        doReturn(List.of(unit(2L, null, "2", "st", true, true))).when(service).getStationsByUserId(1L);
        User u = user(1L, EUnitType.Inner);
        u.setDefaultStation(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        List<StationsOfLoginUserDto> result = service.getStationsForFrontByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().isDefaultBy()).isTrue();
        assertThat(result.getFirst().getValue()).isEqualTo(2L);
        assertThat(result.getFirst().getName()).isEqualTo("st");
    }

    @Test
    @DisplayName("getStationsByCurrUserAndStation：委托仓储并映射")
    void getStationsByCurrUserAndStation() {
        when(repository.getStationsByCurrUserAndStation(1L))
                .thenReturn(List.of(unit(2L, null, "2", "st", true, true)));

        assertThat(service.getStationsByCurrUserAndStation(1L)).hasSize(1);
    }

    @Test
    @DisplayName("getStationTreeByUserId：构建包含子站的最小树")
    void getStationTreeByUserId() {
        OrganizationUnit station = unit(10L, null, "1/10", "st", true, true);
        OrganizationUnit mid = unit(20L, 10L, "1/10/20", "mid", false, true);
        OrganizationUnit leaf = unit(30L, 20L, "1/10/20/30", "leaf", true, true);
        doReturn(List.of(station)).when(service).getStationsByUserId(1L);
        doReturn(List.of(mid, leaf)).when(service).findDescendants(10L);

        List<OrganizationUnit> result = service.getStationTreeByUserId(1L);

        assertThat(result).extracting(OrganizationUnit::getId).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("getStationTreeByUserId：后代不含厂站时不追加中间节点")
    void getStationTreeByUserId_noStationDescendant() {
        OrganizationUnit station = unit(10L, null, "1/10", "st", true, true);
        OrganizationUnit mid = unit(20L, 10L, "1/10/20", "mid", false, true);
        doReturn(List.of(station)).when(service).getStationsByUserId(1L);
        doReturn(List.of(mid)).when(service).findDescendants(10L);

        assertThat(service.getStationTreeByUserId(1L))
                .extracting(OrganizationUnit::getId).containsExactly(10L);
    }

    @Test
    @DisplayName("findByCodes：按机构编码查询")
    void findByCodes() {
        stubFindAllSpec(List.of(unit(1L, null, "1", "A", false, true)));

        assertThat(service.findByCodes(List.of("A"))).hasSize(1);
    }

    @Test
    @DisplayName("setStationEnabled：非厂站不可设置")
    void setStationEnabled_nonStation() {
        doReturn(Optional.of(unit(1L, null, "1", "org", false, true))).when(service).findById(1L);

        assertThatThrownBy(() -> service.setStationEnabled(1L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非厂站");
    }

    @Test
    @DisplayName("setStationEnabled：厂站可切换启用状态")
    void setStationEnabled_station() {
        OrganizationUnit u = unit(1L, null, "1", "st", true, true);
        doReturn(Optional.of(u)).when(service).findById(1L);
        doReturn(u).when(service).save(any(OrganizationUnit.class));

        service.setStationEnabled(1L, false);

        assertThat(u.getStationEnabled()).isFalse();
        verify(service).save(u);
    }

    @Test
    @DisplayName("setStationEnabled：机构不存在抛异常")
    void setStationEnabled_notFound() {
        doReturn(Optional.empty()).when(service).findById(1L);

        assertThatThrownBy(() -> service.setStationEnabled(1L, true))
                .isInstanceOf(BusinessException.class);
    }

    // ------------------------------------------------------------------
    // 查询与映射
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getAllOrgas：映射全部机构")
    void getAllOrgas() {
        doReturn(List.of(unit(1L, null, "1", "org", false, true))).when(service).findAll();

        assertThat(service.getAllOrgas()).hasSize(1);
    }

    @Test
    @DisplayName("findOneByDisplayName：名称空白返回 null")
    void findOneByDisplayName_blank() {
        assertThat(service.findOneByDisplayName(" ")).isNull();
    }

    @Test
    @DisplayName("findOneByDisplayName：无匹配返回 null")
    void findOneByDisplayName_empty() {
        stubFindAllSpec(List.of());

        assertThat(service.findOneByDisplayName("none")).isNull();
    }

    @Test
    @DisplayName("findOneByDisplayName：命中返回 DTO")
    void findOneByDisplayName_found() {
        stubFindAllSpec(List.of(unit(1L, null, "1", "org", false, true)));

        OrganizationUnitDto dto = service.findOneByDisplayName("org");

        assertThat(dto).isNotNull();
        assertThat(dto.getDisplayName()).isEqualTo("org");
    }

    @Test
    @DisplayName("findOneById：id 为空返回 null")
    void findOneById_null() {
        assertThat(service.findOneById(null)).isNull();
    }

    @Test
    @DisplayName("findOneById：未命中返回 null")
    void findOneById_absent() {
        doReturn(Optional.empty()).when(service).findById(3L);

        assertThat(service.findOneById(3L)).isNull();
    }

    @Test
    @DisplayName("findOneById：命中返回 DTO")
    void findOneById_found() {
        doReturn(Optional.of(unit(3L, null, "3", "org3", false, true))).when(service).findById(3L);

        assertThat(service.findOneById(3L).getDisplayName()).isEqualTo("org3");
    }

    @Test
    @DisplayName("findByIds：入参为 null 返回 null")
    void findByIds_null() {
        assertThat(service.findByIds(null)).isNull();
    }

    @Test
    @DisplayName("findByIds：按 id 批量查询并映射")
    void findByIds_maps() {
        when(repository.findByIdIn(List.of(1L))).thenReturn(List.of(unit(1L, null, "1", "org", false, true)));

        assertThat(service.findByIds(List.of(1L))).hasSize(1);
    }

    @Test
    @DisplayName("getOrganizationUnitMapByOrgIds：逐机构展开后代并附带自身")
    void getOrganizationUnitMapByOrgIds() {
        OrganizationUnitDto dto = new OrganizationUnitDto();
        dto.setId(1L);
        doReturn(List.of(dto)).when(service).findByIds(any());
        doReturn(List.of(unit(1L, null, "1", "org", false, true))).when(service).findDescendants(1L);

        Map<Long, List<OrganizationUnitDto>> map = service.getOrganizationUnitMapByOrgIds(List.of(1L));

        assertThat(map).containsKey(1L);
        assertThat(map.get(1L)).hasSize(2);
    }

    @Test
    @DisplayName("getCurrentOrganization：未选择机构返回 null")
    void getCurrentOrganization_noDefault() {
        assertThat(service.getCurrentOrganization()).isNull();
    }

    @Test
    @DisplayName("getCurrentOrganization：所选机构不存在返回 null")
    void getCurrentOrganization_notFound() {
        DataFilterContextHolder.setDataFilterId("5");
        doReturn(Optional.empty()).when(service).findById(5L);

        assertThat(service.getCurrentOrganization()).isNull();
    }

    @Test
    @DisplayName("getCurrentOrganization：命中返回 DTO")
    void getCurrentOrganization_found() {
        DataFilterContextHolder.setDataFilterId("5");
        doReturn(Optional.of(unit(5L, null, "5", "org5", false, true))).when(service).findById(5L);

        assertThat(service.getCurrentOrganization().getDisplayName()).isEqualTo("org5");
    }

    @Test
    @DisplayName("getUserIdsByOrgIdAndNameLike：名称空白且无机构返回 null")
    void getUserIdsByOrgIdAndNameLike_allBlank() {
        assertThat(service.getUserIdsByOrgIdAndNameLike(null, null, false)).isNull();
        assertThat(service.getUserIdsByOrgIdAndNameLike(" ", null, false)).isNull();
    }

    @Test
    @DisplayName("getUserIdsByOrgIdAndNameLike：仅名称查询时机构 id 集合为空")
    void getUserIdsByOrgIdAndNameLike_nameOnly() {
        when(repository.getUserIds(any(), any(), any())).thenReturn(List.of(1L, 2L));

        assertThat(service.getUserIdsByOrgIdAndNameLike("abc", null, false)).hasSize(2);
    }

    @Test
    @DisplayName("getUserIdsByOrgIdAndNameLike：带机构时按机构类型过滤")
    void getUserIdsByOrgIdAndNameLike_withOrg() {
        doReturn(List.of(5L)).when(service).getAllDescendantIds(5L, true);
        doReturn(Optional.of(unit(5L, null, "5", "org5", false, true))).when(service).findById(5L);
        when(repository.getUserIds(any(), any(), any())).thenReturn(List.of(1L));

        assertThat(service.getUserIdsByOrgIdAndNameLike(null, 5L, true)).hasSize(1);
    }

    // ------------------------------------------------------------------
    // 批量保存
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saveAllOrgas：新增与更新分支并存")
    void saveAllOrgas_addAndUpdate() {
        OrganizationUnit existing = unit(7L, 1L, "1/7", "old", false, true);
        when(repository.findAllById(any())).thenReturn(List.of(existing));
        doReturn(Optional.of(unit(3L, null, "3", "p", false, true))).when(service).findById(3L);
        doReturn(List.of()).when(service).getAncestors(3L);
        doReturn(List.of(existing)).when(service).saveAll(any());

        OrganizationUnitDto add = new OrganizationUnitDto();
        add.setDisplayName("new");
        OrganizationUnitDto upd = new OrganizationUnitDto();
        upd.setId(7L);
        upd.setParentId(3L);

        List<OrganizationUnitDto> result = service.saveAllOrgas(new ArrayList<>(List.of(add, upd)));

        assertThat(result).hasSize(1);
        assertThat(existing.getParentId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("saveAllOrgas：空入参返回空列表")
    void saveAllOrgas_empty() {
        assertThat(service.saveAllOrgas(new ArrayList<>())).isEmpty();
        verify(service, never()).saveAll(any());
    }

    // ------------------------------------------------------------------
    // 序列号 / Excel 导入
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getSerialNos：去除前导 0 后返回")
    void getSerialNos_stripsLeadingZeros() {
        OrganizationUnitServiceImpl real = new OrganizationUnitServiceImpl();
        ReflectionTestUtils.setField(real, "serialNoService", serialNoService);
        when(serialNoService.getSerialNos(anyString(), any(EnumResetType.class), anyString(), anyInt(), anyInt()))
                .thenReturn(new String[]{"0001", "0002"});

        String[] result = ReflectionTestUtils.invokeMethod(real, "getSerialNos", 2);

        assertThat(result).containsExactly("1", "2");
    }

    @Test
    @DisplayName("getOrganizationUnitUsers：已指定排序字段时保留原值")
    void getOrganizationUnitUsers_keepsGivenSorting() {
        when(repository.getOrganizationUnitUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        GetOrganizationUnitUsersInput input = usersInput(List.of(2L), false);
        input.setSorting("name");

        service.getOrganizationUnitUsers(input);

        assertThat(input.getSorting()).isEqualTo("name");
    }

    @Test
    @DisplayName("importUnitByExcel：读取流失败包装为业务异常")
    void importUnitByExcel_ioException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> service.importUnitByExcel(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("boom");
    }
}
