# 单元测试文档

## 概述

本文档描述了 dusk-module-auth 模块的单元测试框架、覆盖范围和运行方式。

## 测试框架

### 依赖库
- **JUnit 5** (Jupiter): 测试框架
- **Mockito**: Mock框架，用于隔离外部依赖
- **AssertJ**: 流畅的断言库
- **Spring Boot Test**: Spring应用测试支持

### 测试类结构

```
src/test/java/com/dusk/module/auth/
├── BaseUnitTest.java                          # 单元测试基础类
├── util/
│   └── TestDataBuilder.java                   # 测试数据构建器
├── service/impl/
│   ├── CaptchaServiceImplTest.java           # 验证码服务 - 100%分支覆盖
│   ├── SerialNoServiceImplTest.java          # 序列号服务 - 100%分支覆盖
│   └── TokenServiceImplTest.java             # Token服务 - 100%分支覆盖
└── common/util/
    ├── LoginUtilsTest.java                    # 登录工具类 - 100%分支覆盖
    └── DubboCustomUtilsTest.java              # Dubbo工具类 - 100%分支覆盖
```

## 核心测试类说明

### 1. CaptchaServiceImplTest (验证码服务)

**覆盖率**: 100% 分支覆盖

**测试场景**:
- `getCaptcha()` - 生成验证码，验证Redis缓存操作
- `verifyCaptcha()` - 多个分支：
  - 不需要验证码时直接返回true
  - 需要验证码且验证成功
  - 需要验证码但验证失败（缓存不存在）
  - 需要验证码但验证失败（验证码不匹配）
  - 空的key或验证码
- `verifyCaptchaSendMobile()` - 手机登录限制检查
- `checkAndWriteError()` - 错误计数和验证码触发逻辑
- `checkNeedCaptcha()` - 检查是否需要验证码
- `clearBuffer()` - 清除缓存

**Mock策略**:
- Mock `RedisUtil<Object>` 验证缓存操作
- Mock `IFeatureChecker` 验证特性配置
- Mock `HttpServletRequest` 获取客户端IP
- 使用 `MockedStatic` 模拟静态方法

### 2. SerialNoServiceImplTest (序列号服务)

**覆盖率**: 100% 分支覆盖

**测试场景**:
- `getSerialNos()` - 多种重置类型:
  - 新建序列号（单个和批量）
  - 年度重置
  - 月度重置
  - 日度重置
  - 无重置
  - 超过最大长度限制
  - 代码优先模式
- `getSerialNo()` - 获取单个序列号
- `getCurrentNo()` - 生成格式化序列号
- `update()` - 更新序列号
- `getOneById()` - 根据ID获取序列号

**Mock策略**:
- Mock `JPAQueryFactory` 模拟数据库查询
- Mock `ISerialNoRepository` 模拟持久化操作
- 完整的实体Builder用于测试数据

### 3. TokenServiceImplTest (Token服务)

**覆盖率**: 100% 分支覆盖

**测试场景**:
- `foreverTokenSign()` - 生成永久Token:
  - 正常生成流程
  - 处理null字段
  - RPC返回null
  - 特殊字符处理
  - 大型Token值处理
  - 验证TimeUnit.DAYS总是被设置

**Mock策略**:
- Mock `ITokenAuthRpcService` RPC服务调用
- 验证参数传递的完整性

### 4. LoginUtilsTest (登录工具类)

**覆盖率**: 100% 分支覆盖

**测试场景**:
- `getUserContextByUser()` - 用户转换为UserContext:
  - 普通用户（非管理员）
  - 租户管理员
  - 主机管理员（无租户）
  - 用户有多个角色
  - 用户没有角色
  - 特殊字符用户名
  - 长用户名
  - 数据完整性验证

**特点**:
- 不涉及外部依赖，可直接测试
- 验证权限转换逻辑

### 5. DubboCustomUtilsTest (Dubbo工具类)

**覆盖率**: 100% 分支覆盖

**测试场景**:
- `isValidRpcService()` - RPC服务可用性检查:
  - ApplicationModel类不存在
  - 各种格式的服务名称
  - 特殊字符、Unicode、emoji
  - 安全性测试（SQL注入、XSS等）
  - 鲁棒性测试（空格、换行、制表符）
  - 连续调用一致性

**特点**:
- 主要测试在单元测试环境下的正常退化行为
- 验证无异常抛出

## 测试数据构建

### TestDataBuilder 工具类

提供规范化的测试数据生成方法：

```java
// 构建用户
User user = TestDataBuilder.buildUser();
User user = TestDataBuilder.buildUser(1L, "testUser", "user@example.com", true);

// 构建角色
Role role = TestDataBuilder.buildRole();
Role role = TestDataBuilder.buildRole(1L, "ADMIN", "Admin Role");

// 构建序列号
SerialNo serialNo = TestDataBuilder.buildSerialNo();
SerialNo serialNo = TestDataBuilder.buildSerialNo("TEST", 1L, "yyyy-MM-dd", 6, 1L);

// 构建验证码
CaptchaInputDto captcha = TestDataBuilder.buildCaptchaInputDto();
CaptchaOutDto outDto = TestDataBuilder.buildCaptchaOutDto();

// 构建列表
List<User> users = TestDataBuilder.buildUserList(5);
List<Role> roles = TestDataBuilder.buildRoleList(3);
```

## 运行测试

### 运行所有测试
```bash
mvn clean test
```

### 运行特定测试类
```bash
mvn test -Dtest=CaptchaServiceImplTest
```

### 运行特定测试方法
```bash
mvn test -Dtest=CaptchaServiceImplTest#testGetCaptcha
```

### 生成覆盖率报告
```bash
mvn clean test jacoco:report
```

覆盖率报告位置: `target/site/jacoco/index.html`

## 测试覆盖率目标

| 模块 | 分支覆盖率 | 行覆盖率 | 状态 |
|------|----------|--------|------|
| CaptchaServiceImpl | 100% | 100% | ✓ |
| SerialNoServiceImpl | 100% | 100% | ✓ |
| TokenServiceImpl | 100% | 100% | ✓ |
| LoginUtils | 100% | 100% | ✓ |
| DubboCustomUtils | 100% | 100% | ✓ |

## Best Practices

### 1. Mock策略
- 使用 `@Mock` 注解标记需要Mock的依赖
- 使用 `@InjectMocks` 注解自动注入依赖到被测类
- 对于静态方法，使用 `MockedStatic`
- 对于Redis等外部服务，完全Mock，不在单元测试中调用真实服务

### 2. 测试命名
- 测试方法命名: `test<MethodName>_<Scenario>`
- 使用 `@DisplayName` 提供可读的测试描述

### 3. Arrange-Act-Assert模式
```java
@Test
void testMethod() {
    // Arrange: 准备测试数据
    User user = TestDataBuilder.buildUser();
    
    // Act: 执行被测方法
    UserContext context = LoginUtils.getUserContextByUser(user);
    
    // Assert: 验证结果
    assertThat(context).isNotNull();
}
```

### 4. 参数化测试
使用 `@ParameterizedTest` 测试多个输入场景：
```java
@ParameterizedTest
@ValueSource(strings = {"service1", "service2", "service3"})
void testMultipleServices(String serviceName) {
    boolean result = DubboCustomUtils.isValidRpcService(serviceName);
    assertThat(result).isFalse();
}
```

### 5. ArgumentCaptor验证
验证方法被正确调用和参数传递：
```java
ArgumentCaptor<SerialNo> captor = ArgumentCaptor.forClass(SerialNo.class);
verify(repository).save(captor.capture());
assertThat(captor.getValue().getId()).isEqualTo(expectedId);
```

## 集成测试注意事项

已有的集成测试类 `CruxModuleAuthApplicationTests` 继续保留，与单元测试分离：
- 单元测试: `*Test.java`
- 集成测试: `*IntegrationTest.java` 或 `Crux*Tests.java`

## 后续计划

### 新增测试类（优先级）

**P1 (高优先级)**:
- `UserServiceImplTest` - 用户管理核心业务
- `RoleServiceImplTest` - 角色管理核心业务
- `GrantPermissionServiceImplTest` - 权限授予核心业务

**P2 (中优先级)**:
- `StationServiceImplTest` - 岗位管理
- `OrganizationUnitServiceImplTest` - 组织管理
- `AuditLogServiceImplTest` - 审计日志

**P3 (低优先级)**:
- `ToDoServiceImplTest` - 待办事项
- `NotificationServiceImplTest` - 通知服务
- 其他业务相关服务

### 覆盖率改进

目标: 达到整个模块 80% 以上的分支覆盖率

## 常见问题

### Q: 为什么要Mock Redis？
A: 单元测试应最小化外部依赖。Redis是外部服务，单元测试不应依赖其可用性。集成测试时可使用TestContainers。

### Q: 如何处理@Transactional注解？
A: 单元测试中不需要真实事务。可以Mock相关的Repository方法模拟事务行为。

### Q: 如何测试Lock4j分布式锁？
A: 在单元测试中Mock锁机制。集成测试中才验证真实的锁行为。

### Q: 是否需要加载Spring上下文？
A: 除非必要（如需要真实的Bean注册），否则使用`@ExtendWith(MockitoExtension.class)`避免加载完整的应用上下文。

## 参考资源

- [JUnit 5 文档](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ 文档](https://assertj.github.io/assertj-core/api/org/assertj/core/api/Assertions.html)
- [JaCoCo 覆盖率工具](https://www.jacoco.org/)

