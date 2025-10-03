package com.dusk.module.auth.repository;

import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class IAuditLogRepositoryTest {

    @Autowired
    private IAuditLogRepository auditLogRepository;

    @Autowired
    private IUserRepository userRepository; // 假设有这个仓库类用于测试

    private User testUser;
    private AuditLog testAuditLog1;
    private AuditLog testAuditLog2;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setName("Test User");
        testUser.setUserName("testuser");
        testUser.setEmailAddress("test@example.com");
        userRepository.save(testUser);

        // 创建审计日志1
        testAuditLog1 = new AuditLog();
        testAuditLog1.setServiceName("UserService");
        testAuditLog1.setMethodName("createUser");
        testAuditLog1.setParameters("param1, param2");
        testAuditLog1.setExecutionTime(LocalDateTime.now());
        testAuditLog1.setClientIpAddress("192.168.1.1");
        testAuditLog1.setCreateUser(testUser);
        auditLogRepository.save(testAuditLog1);

        // 创建审计日志2
        testAuditLog2 = new AuditLog();
        testAuditLog2.setServiceName("ProductService");
        testAuditLog2.setMethodName("deleteProduct");
        testAuditLog2.setParameters("productId");
        testAuditLog2.setClientIpAddress("192.168.1.2");
        testAuditLog2.setExecutionTime(LocalDateTime.now().minusDays(1));
        testAuditLog2.setCreateUser(testUser);
        auditLogRepository.save(testAuditLog2);
    }

    @Test
    @DisplayName("测试使用EntityGraph查询审计日志分页")
    void testFindAllWithSpecificationAndPageable() {
        // 创建查询条件 - 查询执行时间超过60ms的日志
        Specification<AuditLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.greaterThan(root.get("executionTime"), 60L));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 创建分页
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testAuditLog1.getId(), result.getContent().get(0).getId());

        // 验证EntityGraph是否正确加载了createUser
        assertNotNull(result.getContent().get(0).getCreateUser());
        assertEquals(testUser.getId(), result.getContent().get(0).getCreateUser().getId());
        assertEquals(testUser.getName(), result.getContent().get(0).getCreateUser().getName());
    }

    @Test
    @DisplayName("测试使用EntityGraph查询服务名称条件")
    void testFindAllWithServiceNameSpecification() {
        // 创建查询条件 - 查询UserService服务的日志
        Specification<AuditLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("serviceName"), "UserService"));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 创建分页
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("createUser", result.getContent().get(0).getMethodName());
    }

    @Test
    @DisplayName("测试使用EntityGraph查询多条件")
    void testFindAllWithMultipleConditions() {
        // 创建查询条件 - 查询执行时间大于30且IP为192.168.1.2的日志
        Specification<AuditLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.greaterThan(root.get("executionTime"), 30L));
            predicates.add(criteriaBuilder.equal(root.get("clientIpAddress"), "192.168.1.2"));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 创建分页
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("deleteProduct", result.getContent().get(0).getMethodName());
    }

    @Test
    @DisplayName("测试空查询条件")
    void testFindAllWithEmptySpecification() {
        // 创建空查询条件
        Specification<AuditLog> spec = (root, query, criteriaBuilder) -> null;

        // 创建分页
        Pageable pageable = PageRequest.of(0, 10);

        // 执行查询
        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }
}

