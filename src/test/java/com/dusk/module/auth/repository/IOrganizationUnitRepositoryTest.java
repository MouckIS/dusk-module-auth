package com.dusk.module.auth.repository;

import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserForSelectDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserInfoListDto;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class IOrganizationUnitRepositoryTest {

    @Autowired
    private IOrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private IUserRepository userRepository; // 假设有这个仓库类用于测试

    private User user1;
    private User user2;
    private User user3;
    private OrganizationUnit dept;
    private OrganizationUnit team;
    private OrganizationUnit station;

    @BeforeEach
    void setUp() {
        // 创建测试用户1
        user1 = new User();
        user1.setName("John Doe");
        user1.setUserName("john.doe");
        user1.setEmailAddress("john.doe@example.com");
        user1.setJob("Developer");
        user1.setPhoneNo("13800138001");
        user1.setUserStatus(UserStatus.OnJob);
        user1.setUserType(EUnitType.Inner);
        userRepository.save(user1);

        // 创建测试用户2
        user2 = new User();
        user2.setName("Jane Smith");
        user2.setUserName("jane.smith");
        user2.setEmailAddress("jane.smith@example.com");
        user2.setJob("Designer");
        user2.setPhoneNo("13800138002");
        user2.setUserStatus(UserStatus.OnJob);
        user2.setUserType(EUnitType.Inner);
        userRepository.save(user2);

        // 创建测试用户3 (无组织)
        user3 = new User();
        user3.setName("Mark Wilson");
        user3.setUserName("mark.wilson");
        user3.setEmailAddress("mark.wilson@example.com");
        user3.setJob("Manager");
        user3.setPhoneNo("13800138003");
        user3.setUserStatus(UserStatus.OnJob);
        user3.setUserType(EUnitType.Inner);
        userRepository.save(user3);

        // 创建测试部门
        dept = new OrganizationUnit();
        dept.setDisplayName("IT Department");
        dept.setType(EUnitType.Inner);
        List<User> deptUsers = new ArrayList<>();
        deptUsers.add(user1);
        deptUsers.add(user2);
        dept.setUsers(deptUsers);
        organizationUnitRepository.save(dept);

        // 创建测试团队
        team = new OrganizationUnit();
        team.setDisplayName("Development Team");
        team.setType(EUnitType.Inner);
        team.setParentId(dept.getId());
        List<User> teamUsers = new ArrayList<>();
        teamUsers.add(user1);
        team.setUsers(teamUsers);
        organizationUnitRepository.save(team);

        // 创建测试工作站
        station = new OrganizationUnit();
        station.setDisplayName("Test Station");
        station.setType(EUnitType.Inner);
        station.setStation(true);
        station.setStationEnabled(true);
        organizationUnitRepository.save(station);
    }

    @Test
    @DisplayName("测试查询组织单元下的用户")
    void testGetOrganizationUnitUsers() {
        // 设置查询参数
        Set<Long> queryOrgaIds = new HashSet<>(Collections.singletonList(dept.getId()));
        String filter = "";
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<OrganizationUnitUserListDto> result = organizationUnitRepository.getOrganizationUnitUsers(queryOrgaIds, filter, pageable, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    @DisplayName("测试使用用户名过滤查询组织单元下的用户")
    void testGetOrganizationUnitUsersWithFilter() {
        // 设置查询参数
        Set<Long> queryOrgaIds = new HashSet<>(Collections.singletonList(dept.getId()));
        String filter = "john";
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<OrganizationUnitUserListDto> result = organizationUnitRepository.getOrganizationUnitUsers(queryOrgaIds, filter, pageable, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("john.doe", result.getContent().get(0).getUserName());
    }

    @Test
    @DisplayName("测试查询组织单元下的用户详细信息")
    void testGetOrganizationUnitUsersInfo() {
        // 设置查询参数
        Set<Long> queryOrgaIds = new HashSet<>(Collections.singletonList(dept.getId()));
        String filter = "";
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<OrganizationUnitUserInfoListDto> result = organizationUnitRepository.getOrganizationUnitUsersInfo(queryOrgaIds, filter, pageable, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        // 验证返回了详细信息
        OrganizationUnitUserInfoListDto userInfo = result.getContent().get(0);
        assertNotNull(userInfo.getJob());
        assertNotNull(userInfo.getPhoneNo());
        assertNotNull(userInfo.getUserStatus());
    }

    @Test
    @DisplayName("测试查询可供选择的组织单元用户")
    void testGetOrganizationUnitUsersForSelect() {
        // 设置查询参数
        Long orgId = dept.getId();
        String filter = "";
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<OrganizationUnitUserForSelectDto> result = organizationUnitRepository.getOrganizationUnitUsersForSelect(orgId, filter, pageable);

        // 验证结果
        assertNotNull(result);
        // 验证返回了不在指定组织中的用户
        assertEquals(1, result.getContent().size());
        assertEquals(user3.getId(), result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("测试根据用户获取组织单元")
    void testGetOrganizationUnitsByUser() {
        // 执行查询
        List<OrganizationUnit> result = organizationUnitRepository.getOrganizationUnitsByUser(user1.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("测试查找工作站")
    void testFindByStation() {
        // 执行查询
        List<OrganizationUnit> result = organizationUnitRepository.findByStation(true);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Station", result.get(0).getDisplayName());
    }

    @Test
    @DisplayName("测试查找启用的工作站")
    void testFindByStationAndStationEnabled() {
        // 执行查询
        List<OrganizationUnit> result = organizationUnitRepository.findByStationAndStationEnabled(true, true);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Station", result.get(0).getDisplayName());
    }

    @Test
    @DisplayName("测试查询用户")
    void testFindUsers() {
        // 设置查询参数
        Set<Long> queryOrgaIds = new HashSet<>(Collections.singletonList(dept.getId()));
        String filter = "";
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<User> result = organizationUnitRepository.findUsers(queryOrgaIds, filter, pageable, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    @DisplayName("测试获取用户ID列表")
    void testGetUserIds() {
        // 设置查询参数
        Set<Long> queryOrgaIds = new HashSet<>(Collections.singletonList(dept.getId()));

        // 执行查询
        List<Long> result = organizationUnitRepository.getUserIds(queryOrgaIds, null, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(user1.getId()));
        assertTrue(result.contains(user2.getId()));
    }

    @Test
    @DisplayName("测试根据父ID获取工作站")
    void testGetStationsByParentId() {
        // 设置查询条件
        station.setParentId(dept.getId());
        organizationUnitRepository.save(station);

        // 执行查询
        List<OrganizationUnit> result = organizationUnitRepository.getStationsByParentId(dept.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Station", result.get(0).getDisplayName());
    }

    @Test
    @DisplayName("测试根据ID列表查找组织单元")
    void testFindByIdIn() {
        // 设置查询条件
        List<Long> ids = Arrays.asList(dept.getId(), team.getId());

        // 执行查询
        List<OrganizationUnit> result = organizationUnitRepository.findByIdIn(ids);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("测试根据类型查找组织单元")
    void testGetOrganizationUnitsByType() {
        // 执行查询
        List<OrganizationUnit> result = organizationUnitRepository.getOrganizationUnitsByType(EUnitType.Inner);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("IT Department", result.get(0).getDisplayName());
    }

    @Test
    @DisplayName("测试根据组织ID查找用户")
    void testFindUsersByOrgId() {
        // 执行查询
        List<User> result = organizationUnitRepository.findUsersByOrgId(dept.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
    }
}

