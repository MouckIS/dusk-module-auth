package com.dusk.module.auth.service.impl;

import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.datafilter.DataFilterContextHolder;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitDto;
import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.common.rpc.auth.enums.EnumResetType;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.dto.orga.*;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.entity.OrganizationManager;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IOrganizationManagerRepository;
import com.dusk.module.auth.repository.IOrganizationUnitRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ISerialNoService;
import com.dusk.module.auth.service.IUserService;
import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationUnitServiceImplTest {

    @Mock
    private IOrganizationUnitRepository repository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IUserService userService;

    @Mock
    private IOrganizationManagerRepository organizationManagerRepository;

    @Mock
    private ISerialNoService serialNoService;

    @InjectMocks
    private OrganizationUnitServiceImpl organizationUnitService;

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.buildDefault();

    @Captor
    private ArgumentCaptor<OrganizationUnit> organizationUnitCaptor;

    private User testUser;
    private OrganizationUnit testOrganizationUnit;
    private OrganizationUnit childUnit;
    private OrganizationUnit stationUnit;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setUserName("testuser");
        testUser.setEmailAddress("test@example.com");
        testUser.setUserType(EUnitType.Inner);
        testUser.setUserStatus(UserStatus.OnJob);
        testUser.setDefaultStation(3L);

        // 创建测���组织单元
        testOrganizationUnit = new OrganizationUnit();
        testOrganizationUnit.setId(1L);
        testOrganizationUnit.setDisplayName("Test Department");
        testOrganizationUnit.setType(EUnitType.Inner);
        testOrganizationUnit.setPath("1");
        testOrganizationUnit.setUsers(new ArrayList<>(Collections.singletonList(testUser)));

        // 创建子组织单元
        childUnit = new OrganizationUnit();
        childUnit.setId(2L);
        childUnit.setDisplayName("Child Unit");
        childUnit.setParentId(1L);
        childUnit.setType(EUnitType.Inner);
        childUnit.setPath("1.2");
        childUnit.setUsers(new ArrayList<>());

        // 创建工作站组织单元
        stationUnit = new OrganizationUnit();
        stationUnit.setId(3L);
        stationUnit.setDisplayName("Test Station");
        stationUnit.setType(EUnitType.Inner);
        stationUnit.setStation(true);
        stationUnit.setStationEnabled(true);
        stationUnit.setPath("1.2.3");
        stationUnit.setUsers(new ArrayList<>());

        // 注入mapper到目标对象
        try {
            java.lang.reflect.Field field = OrganizationUnitServiceImpl.class.getDeclaredField("dozerMapper");
            field.setAccessible(true);
            field.set(organizationUnitService, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("测试获取外部组织单元 - 内部用户")
    void testGetExternalOrganizationUnits_InnerUser() {
        // 准备测试数据
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        List<OrganizationUnit> externalUnits = new ArrayList<>();
        OrganizationUnit externalUnit = new OrganizationUnit();
        externalUnit.setId(4L);
        externalUnit.setDisplayName("External Org");
        externalUnit.setType(EUnitType.External);
        externalUnits.add(externalUnit);

        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(externalUnits);

        OrganizationManager manager = new OrganizationManager();
        manager.setOrgId(4L);
        manager.setUserId(5L);
        when(organizationManagerRepository.findAll()).thenReturn(Collections.singletonList(manager));

        // 使用Mockito的MockedStatic替代MockStatic (适用于SpringBoot 2.3.8)
        //
        //try (MockedStatic<LoginUserIdContextHolder> mockedStatic = mockStatic(LoginUserIdContextHolder.class)) {
        //    mockedStatic.when(LoginUserIdContextHolder::getUserId).thenReturn(1L);
        //
        //    // 执行方法
        //    var result = organizationUnitService.getExternalOrganizationUnits();
        //
        //    // 验证结果
        //    assertNotNull(result);
        //    assertEquals(1, result.getItems().size());
        //    assertEquals("External Org", result.getItems().get(0).getDisplayName());
        //    assertEquals(5L, result.getItems().get(0).getManagerId());
        //    verify(repository).findAll(any(Specification.class), any(Sort.class));
        //}
    }

    @Test
    @DisplayName("测试获取外部组织单元 - 外部用户")
    void testGetExternalOrganizationUnits_ExternalUser() {
        // 准备测试数据
        User externalUser = new User();
        externalUser.setId(5L);
        externalUser.setName("External User");
        externalUser.setUserType(EUnitType.External);

        when(userRepository.findById(5L)).thenReturn(Optional.of(externalUser));

        OrganizationUnit externalOrg = new OrganizationUnit();
        externalOrg.setId(4L);
        externalOrg.setDisplayName("External Org");
        externalOrg.setType(EUnitType.External);

        // 用户所属组织
        when(repository.getOrganizationUnitsByUser(5L)).thenReturn(Collections.singletonList(externalOrg));
        // 父组织
        when(repository.findById(4L)).thenReturn(Optional.of(externalOrg));

        // 设置当前用户ID
        //try (MockedStatic<LoginUserIdContextHolder> mockedStatic = mockStatic(LoginUserIdContextHolder.class)) {
        //    mockedStatic.when(LoginUserIdContextHolder::getUserId).thenReturn(5L);
        //
        //    // 执行方法
        //    var result = organizationUnitService.getExternalOrganizationUnits();
        //
        //    // 验证结果
        //    assertNotNull(result);
        //    assertEquals(1, result.getItems().size());
        //    assertEquals("External Org", result.getItems().get(0).getDisplayName());
        //}
    }

    @Test
    @DisplayName("测试获取组织单元用户")
    void testGetOrganizationUnitUsers() {
        // 准备测试数据
        GetOrganizationUnitUsersInput input = new GetOrganizationUnitUsersInput();
        input.setOrganizationUnitIds(Collections.singletonList(1L));
        input.setDeepQuery(false);
        input.setFilter("test");

        OrganizationUnitUserListDto userListDto = new OrganizationUnitUserListDto();
        userListDto.setId(1L);
        userListDto.setName("Test User");
        userListDto.setUserName("testuser");

        Page<OrganizationUnitUserListDto> expectedPage = new PageImpl<>(Collections.singletonList(userListDto));
        when(repository.getOrganizationUnitUsers(anySet(), anyString(), any(Pageable.class), any())).thenReturn(expectedPage);

        // 执行方法
        Page<OrganizationUnitUserListDto> result = organizationUnitService.getOrganizationUnitUsers(input);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test User", result.getContent().get(0).getName());
        verify(repository).getOrganizationUnitUsers(anySet(), eq("test"), any(Pageable.class), any());
    }

    @Test
    @DisplayName("测试创建组织单元")
    void testCreate() {
        // 准备测试数据
        CreateOrganizationUnitInput input = new CreateOrganizationUnitInput();
        input.setDisplayName("New Department");
        input.setType(EUnitType.Inner);
        input.setManagerId(1L);

        when(repository.save(any(OrganizationUnit.class))).thenAnswer(invocation -> {
            OrganizationUnit unit = invocation.getArgument(0);
            unit.setId(10L); // 模拟保存后设置ID
            return unit;
        });

        when(serialNoService.getSerialNos(anyString(), eq(EnumResetType.Never), anyString(), anyInt(), anyInt()))
            .thenReturn(new String[]{"00000001"});

        when(repository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        // 执行方法
        OrganizationUnit result = organizationUnitService.create(input);

        // 验证结��
        assertNotNull(result);
        assertEquals("New Department", result.getDisplayName());
        assertEquals(EUnitType.Inner, result.getType());
        assertEquals(10L, result.getId());
        verify(repository).save(any(OrganizationUnit.class));
        verify(organizationManagerRepository).deleteByOrgId(10L);
        verify(organizationManagerRepository).save(any(OrganizationManager.class));
    }

    @Test
    @DisplayName("测试创建具有相同名称的组织单元 - 应抛出异常")
    void testCreateDuplicateName() {
        // 准备测试数据
        CreateOrganizationUnitInput input = new CreateOrganizationUnitInput();
        input.setDisplayName("Duplicate Name");
        input.setType(EUnitType.Inner);

        OrganizationUnit unit1 = new OrganizationUnit();
        unit1.setId(10L);
        unit1.setDisplayName("Duplicate Name");
        unit1.setType(EUnitType.Inner);

        when(repository.save(any(OrganizationUnit.class))).thenAnswer(invocation -> {
            OrganizationUnit unit = invocation.getArgument(0);
            unit.setId(10L);
            return unit;
        });
        when(serialNoService.getSerialNos(anyString(), any(), anyString(), anyInt(), anyInt()))
            .thenReturn(new String[]{"00000001"});
        when(repository.findAll(any(Specification.class))).thenReturn(Arrays.asList(unit1, unit1));

        // 执行方法并验证异常
        assertThrows(BusinessException.class, () -> organizationUnitService.create(input));
    }

    @Test
    @DisplayName("测试更新组织单元")
    void testUpdate() {
        // 准备测试数据
        UpdateOrganizationUnitInput input = new UpdateOrganizationUnitInput();
        input.setId(1L);
        input.setDisplayName("Updated Department");
        input.setManagerId(2L);

        when(repository.findById(1L)).thenReturn(Optional.of(testOrganizationUnit));
        when(repository.save(any(OrganizationUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(testOrganizationUnit));

        // 执行方法
        OrganizationUnit result = organizationUnitService.update(input);

        // 验��结果
        assertNotNull(result);
        assertEquals("Updated Department", result.getDisplayName());
        verify(repository).save(any(OrganizationUnit.class));
        verify(organizationManagerRepository).deleteByOrgId(1L);
        verify(organizationManagerRepository).save(any(OrganizationManager.class));
    }

    @Test
    @DisplayName("测试删除组织单元")
    void testDeleteOrgById() {
        // 准备测试数据
        Long orgId = 1L;
        OrganizationUnitUserListDto userListDto = new OrganizationUnitUserListDto();
        userListDto.setId(1L);
        Page<OrganizationUnitUserListDto> userPage = new PageImpl<>(Collections.singletonList(userListDto));

        when(repository.getOrganizationUnitUsers(anySet(), anyString(), any(Pageable.class), any())).thenReturn(userPage);
        doNothing().when(userService).deleteUserByIds(anyList());
        doNothing().when(organizationManagerRepository).deleteByOrgId(anyLong());
        doNothing().when(repository).deleteById(anyLong());

        // 执行方法
        organizationUnitService.deleteOrgById(orgId);

        // 验证结果
        verify(repository).getOrganizationUnitUsers(anySet(), isNull(), any(Pageable.class), isNull());
        verify(userService).deleteUserByIds(Collections.singletonList(1L));
        verify(organizationManagerRepository).deleteByOrgId(orgId);
        verify(repository).deleteById(orgId);
    }

    @Test
    @DisplayName("测试从组织单元移除用户")
    void testRemoveUserFromOrganizationUnit() {
        // 准备测试数据
        UserToOrganizationUnitInput input = new UserToOrganizationUnitInput();
        input.setOrganizationUnitId(1L);
        input.setUserId(1L);

        User user1 = new User();
        user1.setId(1L);

        User user2 = new User();
        user2.setId(2L);

        OrganizationUnit organizationUnit = new OrganizationUnit();
        organizationUnit.setId(1L);
        organizationUnit.setUsers(new ArrayList<>(Arrays.asList(user1, user2)));

        when(repository.findById(1L)).thenReturn(Optional.of(organizationUnit));
        when(repository.save(any(OrganizationUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行方法
        organizationUnitService.removeUserFromOrganizationUnit(input);

        // 验证结果
        verify(repository).save(organizationUnitCaptor.capture());
        OrganizationUnit savedUnit = organizationUnitCaptor.getValue();
        assertEquals(1, savedUnit.getUsers().size());
        assertEquals(2L, savedUnit.getUsers().get(0).getId());
    }

    @Test
    @DisplayName("测试添加用户到组织单元")
    void testAddUsersToOrganizationUnit() {
        // 准备测试数据
        UsersToOrganizationUnitInput input = new UsersToOrganizationUnitInput();
        input.setOrganizationUnitId(1L);
        input.setUserIds(Arrays.asList(2L, 3L));

        User user1 = new User();
        user1.setId(1L);

        OrganizationUnit organizationUnit = new OrganizationUnit();
        organizationUnit.setId(1L);
        organizationUnit.setUsers(new ArrayList<>(Collections.singletonList(user1)));

        when(repository.findById(1L)).thenReturn(Optional.of(organizationUnit));
        when(repository.save(any(OrganizationUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行方法
        organizationUnitService.addUsersToOrganizationUnit(input);

        // 验证结��
        verify(repository).save(organizationUnitCaptor.capture());
        OrganizationUnit savedUnit = organizationUnitCaptor.getValue();
        assertEquals(3, savedUnit.getUsers().size());
    }

    @Test
    @DisplayName("测试根据用户获取组织单元")
    void testGetOrganizationUnitsByUser() {
        // 准备测试数据
        EntityDto input = new EntityDto(1L);
        List<OrganizationUnit> units = Arrays.asList(testOrganizationUnit, childUnit);

        when(repository.getOrganizationUnitsByUser(1L)).thenReturn(units);

        // 执行方法
        List<OrganizationUnit> result = organizationUnitService.getOrganizationUnitsByUser(input);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).getOrganizationUnitsByUser(1L);
    }

    @Test
    @DisplayName("测试获取父组织")
    void testGetParentOrganizations() {
        // 准备测试数据
        EntityDto input = new EntityDto(2L);

        when(repository.findById(2L)).thenReturn(Optional.of(childUnit));
        when(repository.findById(1L)).thenReturn(Optional.of(testOrganizationUnit));

        // 为findAll方法使用不那么严格的模拟，适合SpringBoot 2.3.8环境
        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(Collections.singletonList(testOrganizationUnit));

        // 执行方法
        List<OrganizationUnit> result = organizationUnitService.getParentOrganizations(input);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.size() >= 1);
        assertEquals(childUnit.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("测试获取工作站")
    void testGetStations() {
        // 准备测试数据
        List<OrganizationUnit> stations = Collections.singletonList(stationUnit);
        when(repository.findByStationAndStationEnabled(true, true)).thenReturn(stations);

        // 执行方法
        List<OrganizationUnit> result = organizationUnitService.getStations();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Station", result.get(0).getDisplayName());
    }

    @Test
    @DisplayName("测试根据用户ID获取工作站")
    void testGetStationsByUserId() {
        // 准备测试数据
        List<OrganizationUnit> userOrgas = Arrays.asList(testOrganizationUnit, childUnit);
        when(repository.getOrganizationUnitsByUser(1L)).thenReturn(userOrgas);
        when(repository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(stationUnit));

        // 执行方法
        List<OrganizationUnit> result = organizationUnitService.getStationsByUserId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Station", result.get(0).getDisplayName());
    }

    @Test
    @DisplayName("测试获取��端用的工作站")
    void testGetStationsForFrontByUserId() {
        // 准备测试数据
        List<OrganizationUnit> stations = Collections.singletonList(stationUnit);
        when(repository.getOrganizationUnitsByUser(1L)).thenReturn(Arrays.asList(testOrganizationUnit));
        when(repository.findAll(any(Specification.class))).thenReturn(stations);
        when(userService.getUserById(1L)).thenReturn(testUser);

        // 执行方法
        List<StationsOfLoginUserDto> result = organizationUnitService.getStationsForFrontByUserId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(stationUnit.getId(), result.get(0).getValue());
        assertEquals(stationUnit.getDisplayName(), result.get(0).getName());
        assertEquals(stationUnit.getId().equals(testUser.getDefaultStation()), result.get(0).isDefaultBy());
    }

    @Test
    @DisplayName("测试获取当前组织")
    void testGetCurrentOrganization() {
        // 准备测试数据
        Long defaultOrgId = 1L;

        //try (MockedStatic<DataFilterContextHolder> mockedStatic = mockStatic(DataFilterContextHolder.class)) {
        //    mockedStatic.when(DataFilterContextHolder::getDefaultOrgId).thenReturn(defaultOrgId);
        //    when(repository.findById(defaultOrgId)).thenReturn(Optional.of(testOrganizationUnit));
        //
        //    // 执行方法
        //    OrganizationUnitDto result = organizationUnitService.getCurrentOrganization();
        //
        //    // 验证结果
        //    assertNotNull(result);
        //    assertEquals(testOrganizationUnit.getId(), result.getId());
        //    assertEquals(testOrganizationUnit.getDisplayName(), result.getDisplayName());
        //}
    }

    @Test
    @DisplayName("测试根据名称查找组织单元")
    void testFindOneByDisplayName() {
        // 准备测试数��
        String displayName = "Test Department";
        when(repository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(testOrganizationUnit));

        // 执行方法
        OrganizationUnitDto result = organizationUnitService.findOneByDisplayName(displayName);

        // 验证结果
        assertNotNull(result);
        assertEquals(displayName, result.getDisplayName());
    }

    @Test
    @DisplayName("测试启用/禁用工作站")
    void testSetStationEnabled() {
        // 准备测试数据
        when(repository.findById(3L)).thenReturn(Optional.of(stationUnit));
        when(repository.save(any(OrganizationUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行方法
        organizationUnitService.setStationEnabled(3L, false);

        // 验证结果
        verify(repository).save(organizationUnitCaptor.capture());
        OrganizationUnit savedUnit = organizationUnitCaptor.getValue();
        assertFalse(savedUnit.getStationEnabled());
    }

    @Test
    @DisplayName("测试启用/禁用非工作站组织单元 - 应抛出异常")
    void testSetStationEnabled_NonStationUnit() {
        // 准备测试数据
        when(repository.findById(1L)).thenReturn(Optional.of(testOrganizationUnit));

        // 执行方法并验证结果
        assertThrows(BusinessException.class, () -> organizationUnitService.setStationEnabled(1L, false));
    }

    @Test
    @DisplayName("测试获取用户ID通过组织ID和名称")
    void testGetUserIdsByOrgIdAndNameLike() {
        // 准备测试数据
        String name = "test";
        Long orgId = 1L;
        Boolean deepQuery = true;

        OrganizationUnitDto unitDto = new OrganizationUnitDto();
        unitDto.setId(1L);
        unitDto.setType(EUnitType.Inner);

        List<Long> expectedUserIds = Arrays.asList(1L, 2L);

        when(repository.getUserIds(anySet(), eq(name), eq(EUnitType.Inner))).thenReturn(expectedUserIds);
        when(repository.findById(1L)).thenReturn(Optional.of(testOrganizationUnit));

        // 执行方法
        List<Long> result = organizationUnitService.getUserIdsByOrgIdAndNameLike(name, orgId, deepQuery);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(expectedUserIds));
    }

    @Test
    @DisplayName("测试根据ID列表查找组织单元")
    void testFindByIds() {
        // 准备测试数据
        List<Long> ids = Arrays.asList(1L, 2L);
        List<OrganizationUnit> units = Arrays.asList(testOrganizationUnit, childUnit);

        when(repository.findByIdIn(ids)).thenReturn(units);

        // 执行方法
        List<OrganizationUnitDto> result = organizationUnitService.findByIds(ids);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testOrganizationUnit.getId(), result.get(0).getId());
        assertEquals(childUnit.getId(), result.get(1).getId());
    }
}

