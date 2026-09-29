package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.auth.dto.station.StationDto;
import com.dusk.common.rpc.auth.enums.EnumResetType;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.dto.station.AddUsersToStationInput;
import com.dusk.module.auth.dto.station.CreateOrUpdateStationInput;
import com.dusk.module.auth.dto.station.GetNotAssignedStationUsersInput;
import com.dusk.module.auth.dto.station.GetStationUsersInput;
import com.dusk.module.auth.dto.station.RemoveUserFromStationInput;
import com.dusk.module.auth.dto.station.StationUserDto;
import com.dusk.module.auth.dto.station.StationUserListDto;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IStationRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ISerialNoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link StationServiceImpl} 单元测试，目标：覆盖 service.impl 门禁包的厂站管理与用户关联逻辑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StationServiceImplTest {

    @Mock
    private IStationRepository repository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private IDataFilterDefinitionContext dataFilterDefinitionContext;
    @Mock
    private ISerialNoService serialNoService;

    private StationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new StationServiceImpl());
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "dataFilterDefinitionContext", dataFilterDefinitionContext);
        ReflectionTestUtils.setField(service, "serialNoService", serialNoService);
    }

    @AfterEach
    void tearDown() {
        // 无 ThreadLocal 状态需要清理
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static Station station(Long id, Long parentId, String path, String displayName) {
        Station s = new Station();
        s.setId(id);
        s.setParentId(parentId);
        s.setPath(path);
        s.setDisplayName(displayName);
        s.setSortIndex(1);
        s.setUsers(new ArrayList<>());
        return s;
    }

    private static User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setName("u" + id);
        u.setUserName("user" + id);
        u.setUserRoles(new ArrayList<>());
        return u;
    }

    private static GetStationUsersInput stationUsersInput(List<Long> ids, boolean deep) {
        GetStationUsersInput input = new GetStationUsersInput();
        input.setStationIds(ids == null ? new ArrayList<>() : new ArrayList<>(ids));
        input.setDeepQuery(deep);
        return input;
    }

    // ------------------------------------------------------------------
    // 用户关联
    // ------------------------------------------------------------------

    @Test
    @DisplayName("removeUserFromStation：剔除指定用户并保存")
    void removeUserFromStation() {
        Station s = station(1L, null, "1", "st");
        s.setUsers(new ArrayList<>(List.of(user(1L), user(2L))));
        doReturn(Optional.of(s)).when(service).findById(1L);
        doReturn(s).when(service).save(any(Station.class));
        RemoveUserFromStationInput input = new RemoveUserFromStationInput();
        input.setStationId(1L);
        input.setUserId(1L);

        service.removeUserFromStation(input);

        assertThat(s.getUsers()).extracting(User::getId).containsExactly(2L);
        verify(service).save(s);
    }

    @Test
    @DisplayName("removeUserFromStation：厂站不存在抛业务异常")
    void removeUserFromStation_notFound() {
        doReturn(Optional.empty()).when(service).findById(1L);
        RemoveUserFromStationInput input = new RemoveUserFromStationInput();
        input.setStationId(1L);

        assertThatThrownBy(() -> service.removeUserFromStation(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未找到相应的厂站");
    }

    @Test
    @DisplayName("addUsersToStation：仅补充未关联用户")
    void addUsersToStation() {
        Station s = station(1L, null, "1", "st");
        s.setUsers(new ArrayList<>(List.of(user(1L))));
        doReturn(Optional.of(s)).when(service).findById(1L);
        doReturn(s).when(service).save(any(Station.class));
        AddUsersToStationInput input = new AddUsersToStationInput();
        input.setStationId(1L);
        input.setUserIds(new ArrayList<>(List.of(1L, 2L)));

        service.addUsersToStation(input);

        assertThat(s.getUsers()).extracting(User::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("addUsersToStation：厂站不存在抛业务异常")
    void addUsersToStation_notFound() {
        doReturn(Optional.empty()).when(service).findById(1L);
        AddUsersToStationInput input = new AddUsersToStationInput();
        input.setStationId(1L);

        assertThatThrownBy(() -> service.addUsersToStation(input))
                .isInstanceOf(BusinessException.class);
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getStationUsers：非深查直接使用入参厂站 id")
    void getStationUsers_plain() {
        when(repository.getStationUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        GetStationUsersInput input = stationUsersInput(List.of(2L), false);

        assertThat(service.getStationUsers(input).getContent()).isEmpty();
        verify(repository).getStationUsers(
                org.mockito.ArgumentMatchers.argThat(set -> set.contains(2L)), any(), any(), any());
    }

    @Test
    @DisplayName("getStationUsers：厂站 id 为空补 -1 哨兵")
    void getStationUsers_emptyIds() {
        when(repository.getStationUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        GetStationUsersInput input = stationUsersInput(null, false);

        service.getStationUsers(input);

        verify(repository).getStationUsers(
                org.mockito.ArgumentMatchers.argThat(set -> set.contains(-1L)), any(), any(), any());
    }

    @Test
    @DisplayName("getStationUsers：深查展开所有后代厂站 id")
    void getStationUsers_deep() {
        doReturn(List.of(2L, 3L)).when(service).getAllDescendantIds(2L, true);
        when(repository.getStationUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        GetStationUsersInput input = stationUsersInput(List.of(2L), true);

        service.getStationUsers(input);

        verify(repository).getStationUsers(
                org.mockito.ArgumentMatchers.argThat(set -> set.contains(2L) && set.contains(3L)), any(), any(), any());
    }

    @Test
    @DisplayName("getStationUsers：返回分页内容")
    void getStationUsers_content() {
        StationUserListDto dto = new StationUserListDto();
        when(repository.getStationUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto)));

        Page<StationUserListDto> result = service.getStationUsers(stationUsersInput(List.of(2L), false));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getNotAssignedStationUsers：未指定厂站返回空分页")
    void getNotAssignedStationUsers_nullStation() {
        assertThat(service.getNotAssignedStationUsers(new GetNotAssignedStationUsersInput()).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("getNotAssignedStationUsers：指定厂站委托仓储")
    void getNotAssignedStationUsers_withStation() {
        when(repository.getNotAssignedStationUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new StationUserDto())));
        GetNotAssignedStationUsersInput input = new GetNotAssignedStationUsersInput();
        input.setStationId(3L);

        assertThat(service.getNotAssignedStationUsers(input).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getAllStations：按排序映射全部厂站")
    void getAllStations() {
        doReturn(List.of(station(1L, null, "1", "s1"))).when(service)
                .findAll(any(Sort.class));

        List<StationDto> result = service.getAllStations();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findOneByDisplayName：名称为空返回 null")
    void findOneByDisplayName_blank() {
        assertThat(service.findOneByDisplayName(" ")).isNull();
    }

    @Test
    @DisplayName("findOneByDisplayName：未命中返回 null")
    void findOneByDisplayName_empty() {
        doReturn(List.of()).when(service).findAll(any(Specification.class));

        assertThat(service.findOneByDisplayName("none")).isNull();
    }

    @Test
    @DisplayName("findOneByDisplayName：命中返回 DTO")
    void findOneByDisplayName_found() {
        doReturn(List.of(station(1L, null, "1", "st"))).when(service).findAll(any(Specification.class));

        StationDto dto = service.findOneByDisplayName("st");

        assertThat(dto).isNotNull();
        assertThat(dto.getDisplayName()).isEqualTo("st");
    }

    @Test
    @DisplayName("getStationsForFrontByUserId：映射下拉项并识别集控站与默认站")
    void getStationsForFrontByUserId() {
        Station parent = station(1L, null, "1", "parent");
        Station child = station(2L, 1L, "1/2", "child");
        when(repository.getStationsByUser(9L)).thenReturn(List.of(parent, child));
        doReturn(List.of(1L, 2L)).when(service).getAllDescendantIds(anyLong(), anyBoolean());
        doReturn(List.of(parent, child)).when(service).findAll(any(Specification.class));
        User u = user(9L);
        u.setDefaultStation(1L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(u));

        List<StationsOfLoginUserDto> result = service.getStationsForFrontByUserId(9L);

        assertThat(result).hasSize(2);
        StationsOfLoginUserDto first = result.getFirst();
        assertThat(first.isMainStation()).isTrue();
        assertThat(first.isDefaultBy()).isTrue();
    }

    @Test
    @DisplayName("getStationsForFrontByUserId：用户不存在抛业务异常")
    void getStationsForFrontByUserId_userNotFound() {
        when(repository.getStationsByUser(9L)).thenReturn(List.of());
        doReturn(List.of()).when(service).getAllDescendantIds(anyLong(), anyBoolean());
        doReturn(List.of()).when(service).findAll(any(Specification.class));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStationsForFrontByUserId(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未找到相应的用户");
    }

    // ------------------------------------------------------------------
    // 序列号 / 树方法
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getSerialNos：去除前导 0")
    void getSerialNos_stripsLeadingZeros() {
        when(serialNoService.getSerialNos(anyString(), any(EnumResetType.class), anyString(), anyInt(), anyInt()))
                .thenReturn(new String[]{"0007", "0008"});
        StationServiceImpl plain = new StationServiceImpl();
        ReflectionTestUtils.setField(plain, "serialNoService", serialNoService);

        String[] result = ReflectionTestUtils.invokeMethod(plain, "getSerialNos", 2);

        assertThat(result).containsExactly("7", "8");
    }

    @Test
    @DisplayName("createOrUpdate：更新已有厂站并刷新数据过滤器")
    void createOrUpdate_update() {
        Station existing = station(5L, null, "5", "old");
        doReturn(Optional.of(existing)).when(service).findById(5L);
        doReturn(existing).when(service).save(any(Station.class));
        doReturn(List.of(existing)).when(service).findAll(any(Specification.class));
        CreateOrUpdateStationInput input = new CreateOrUpdateStationInput();
        input.setId(5L);
        input.setDisplayName("new");

        service.createOrUpdate(input);

        verify(dataFilterDefinitionContext).refresh();
    }

    @Test
    @DisplayName("deleteById：删除后刷新数据过滤器")
    void deleteById_refreshes() {
        Station existing = station(6L, null, "6", "st");
        doReturn(Optional.of(existing)).when(service).findById(6L);
        doNothing().when(service).delete(any(Station.class));

        service.deleteById(6L);

        verify(dataFilterDefinitionContext).refresh();
    }

    @Test
    @DisplayName("saveAll：批量保存后刷新数据过滤器")
    void saveAll_refreshes() {
        Station s = station(7L, null, "7", "st");
        s.setSerialNo("0001");
        when(repository.findAllById(any())).thenReturn(List.of(s));
        when(repository.saveAll(any())).thenReturn(List.of(s));

        service.saveAll(new ArrayList<>(List.of(s)));

        verify(dataFilterDefinitionContext).refresh();
    }
}
