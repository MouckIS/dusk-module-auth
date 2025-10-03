package com.dusk.module.auth.repository;

import com.dusk.module.auth.entity.CommonFavorite;
import com.dusk.module.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Example;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = true)
class ICommonFavoriteRepositoryTest {

    @Autowired
    private ICommonFavoriteRepository commonFavoriteRepository;

    @Autowired
    private IUserRepository userRepository; // 假设有这个仓库类用于测试

    private User testUser;
    private CommonFavorite favorite1;
    private CommonFavorite favorite2;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setName("Test User");
        testUser.setUserName("testuser");
        testUser.setEmailAddress("test@example.com");
        userRepository.save(testUser);

        // 创建测试收藏项1
        favorite1 = new CommonFavorite();
        favorite1.setId(testUser.getId());
        favorite1.setType("report");
        favorite1.setName("Monthly Sales Report");
        //favorite1.("/reports/monthly-sales");
        //favorite1.setIconCls("report-icon");
        //favorite1.setOrderNum(1);
        commonFavoriteRepository.save(favorite1);

        // 创建测试收藏项2
        favorite2 = new CommonFavorite();
        //favorite2.setUserId(testUser.getId());
        favorite2.setType("dashboard");
        favorite2.setName("Main Dashboard");
        //favorite2.setUri("/dashboard/main");
        //favorite2.setIconCls("dashboard-icon");
        //favorite2.setOrderNum(2);
        commonFavoriteRepository.save(favorite2);
    }

    @Test
    @DisplayName("测试保存收藏项")
    void testSave() {
        // 创建新收藏项
        CommonFavorite newFavorite = new CommonFavorite();
        //newFavorite.setUserId(testUser.getId());
        newFavorite.setType("form");
        newFavorite.setName("Customer Registration");
        //newFavorite.setUri("/forms/customer");
        //newFavorite.setIconCls("form-icon");
        //newFavorite.setOrderNum(3);

        // 保存收藏项
        CommonFavorite savedFavorite = commonFavoriteRepository.save(newFavorite);

        // 验证结果
        assertNotNull(savedFavorite.getId());
        assertEquals("Customer Registration", savedFavorite.getName());
    }

    @Test
    @DisplayName("测试根据ID查找收藏项")
    void testFindById() {
        // 查找已存在的收藏项
        Optional<CommonFavorite> foundFavorite = commonFavoriteRepository.findById(favorite1.getId());

        // 验证结果
        assertTrue(foundFavorite.isPresent());
        assertEquals("Monthly Sales Report", foundFavorite.get().getName());
    }

    @Test
    @DisplayName("测试查找全部收藏项")
    void testFindAll() {
        // 查找所有收藏项
        List<CommonFavorite> favorites = commonFavoriteRepository.findAll();

        // 验证结果
        assertNotNull(favorites);
        assertEquals(2, favorites.size());
    }

    @Test
    @DisplayName("测试根据条件查找收藏项")
    void testFindByExample() {
        // 创建查询条件
        CommonFavorite example = new CommonFavorite();
        example.setType("dashboard");

        // 执行查询
        List<CommonFavorite> results = commonFavoriteRepository.findAll(Example.of(example));

        // 验证结果
        assertEquals(1, results.size());
        assertEquals("Main Dashboard", results.get(0).getName());
    }

    @Test
    @DisplayName("测试更新收藏项")
    void testUpdate() {
        // 修改收藏项
        favorite1.setName("Updated Report Name");
        //favorite1.setOrderNum(10);

        // 保存修改
        CommonFavorite updatedFavorite = commonFavoriteRepository.save(favorite1);

        // 验证结果
        assertEquals("Updated Report Name", updatedFavorite.getName());
        //assertEquals(10, updatedFavorite.getOrderNum());

        // 从数据库再次查询确认更新成功
        Optional<CommonFavorite> reloadedFavorite = commonFavoriteRepository.findById(favorite1.getId());
        assertTrue(reloadedFavorite.isPresent());
        assertEquals("Updated Report Name", reloadedFavorite.get().getName());
    }

    @Test
    @DisplayName("测试删除收藏项")
    void testDelete() {
        // 删除收藏项
        commonFavoriteRepository.delete(favorite2);

        // 验证删除成功
        Optional<CommonFavorite> shouldBeEmpty = commonFavoriteRepository.findById(favorite2.getId());
        assertFalse(shouldBeEmpty.isPresent());

        // 验证其他收藏项不受影响
        List<CommonFavorite> remainingFavorites = commonFavoriteRepository.findAll();
        assertEquals(1, remainingFavorites.size());
    }
}

