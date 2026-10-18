# AGENTS.md — dusk-module-auth

> 认证授权服务。它的 RPC 契约被网关、workflow、notification、minio 等模块消费，改接口前务必确认下游。
> 用户文档见 [`README.md`](README.md)，工作空间约束见 [`../AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

多租户 SaaS 的认证与授权中心：登录、JWT、用户/角色/租户/组织、权限树、特性开关、设置、待办、审计。

仓库为双模块：

```text
dusk-module-auth/
├── dusk-module-auth/          可运行服务（Spring Boot）
└── dusk-module-auth-shared/   Dubbo RPC 契约 + DTO（70 个类）
```

## 2. 构建 / 运行 / 测试

```bash
export GH_PAT=<read:packages PAT>

# 构建（模块自带 settings.xml）
mvn -B -ntp -U -s settings.xml -DskipTests clean package

# 运行（默认 sit；本地用 dev，端口 53201）
mvn -s settings.xml spring-boot:run -Dspring-boot.run.profiles=dev

# 测试（44 个测试类，含 JaCoCo 门槛）
mvn -s settings.xml test

# 单个测试
mvn -s settings.xml test -Dtest=UserServiceImplTest
```

- JDK 25、Spring Boot 4.0.8、Maven 3.9+。
- 运行时依赖 PostgreSQL、Redis、Nacos；RabbitMQ/MQTT 可选择开启。
- `CruxModuleAuthApplicationTests` 被 surefire 排除（需真实中间件）；其余测试为 Mockito 单元测试。
- JaCoCo：`service.impl` 与 `common.util` 分支 ≥ 60%、行 ≥ 70%。

## 3. 代码结构

```text
com/dusk/module/auth/
├── controller/       REST 控制器（约 27 个）
├── service/          业务接口 + RPC 接口（本地定义）
├── service/impl/     业务实现 + @DubboService RPC 实现
├── repository/       Spring Data JPA（继承 IBaseRepository）
├── entity/           JPA 实体（27+，含 dashboard 子包）
├── mapper/           MapStruct（INSTANCE），**不是 MyBatis**
├── dto/              按领域拆分的入参/出参
├── common/           Security 过滤器链、权限缓存、数据权限、工具
├── authorization/    23 个 *AuthProvider（权限树）
├── feature/ + impl/  特性开关
├── setting/          设置注册表
├── syscode/          系统码
├── cache/            Redis + 内存兜底缓存
├── manage/           @Transactional 跨实体操作
├── aspect/           UserLogAspect / WxLoginAspect
├── excel/            导入监听器/导出器
├── igw/              i国网 SSO
├── listener/         消息监听
└── DuskAuthApplication.java
```

分层规则：`controller → service → service.impl → repository → PostgreSQL`。

- 复杂查询用 QueryDSL（`JPAQueryFactory` + `Q*`）。
- 实体抓取用 `@EntityGraph` 控制（如 `User.role`）。
- DTO 转换走 MapStruct，禁止手写大段 setter。

## 4. 登录与鉴权（最重要）

```text
1. POST /login（账号/密码/验证码，密码 SM4）
2. JWTAuthenticationFilter（common/filter/）校验验证码 → DefaultAuthenticationProvider 校验凭证
3. 成功签发 JWT；Success/FailureHandler 记录登录与审计日志
4. 网关调用 IAuthRpcService.auth(authorization, applicationName, url)
5. TokenAuthManager 校验 Token
6. DefaultInvocationSecurityMetadataSource 解析 URL 权限
7. DefaultAccessDecisionManager 决策
8. UserContext / TenantContextHolder 在业务层提供上下文
```

关键类：

| 关注点            | 文件                                                           |
|-------------------|----------------------------------------------------------------|
| Security 过滤器链 | `common/config/SecurityConfiguration.java`                     |
| 登录入口          | `common/filter/JWTAuthenticationFilter.java`                   |
| 认证 Provider     | `common/provider/DefaultAuthenticationProvider.java`           |
| Token 管理        | `common/manage/TokenAuthManager.java`                          |
| 权限决策          | `common/manage/DefaultAccessDecisionManager.java`              |
| URL 权限元数据    | `common/metadata/DefaultInvocationSecurityMetadataSource.java` |
| 数据权限          | `common/datafilter/DataFilterDefinitionContext.java`           |
| 权限缓存          | `common/permission/`                                           |

**注意：Controller 层默认 `permitAll`，真正的强制鉴权发生在网关调用 auth RPC 的边界。**

## 5. 权限 / 特性 / 设置

- 权限树：`*AuthProvider` 用 `createChildPermission("Pages.Xxx", ...)` 声明；启动时展平进 Redis 权限缓存（内存兜底）。
- 新增页面/按钮权限 = 新增或修改 Provider 常量，不需要建权限表。
- 特性开关：`*FeatureProvider` 声明 key/默认值/UI 类型，启动收集后写入 Redis；`FeatureChecker` 读取。
- 设置：`*SettingProvider` 注册设置项，`SettingManagerDefault` 管理，`RedisSettingsCache` 缓存，`MinorSettingsPublish`
  推送到其他模块。

## 6. 对外 RPC

- 契约在 `dusk-module-auth-shared`（`I*RpcService`），服务端实现位于 `service/impl`，`@DubboService` 注册；扫描包
  `com.dusk.module.auth.service`。
- 修改契约（方法签名/出入参）会影响网关与其他模块：必须同步发布 shared 构件并升级消费方。
- 消费其他模块：`@DubboReference`，注意区分本模块自引用与 metadata/workflow 等外部契约。

## 7. 数据库与迁移

- 脚本目录：`src/main/resources/db/migration/`，当前为 `V1`~`V18`（缺 V7/V11）+ 30 余个 `R__` 可重复脚本。
- 历史表：`flyway_schema_history_auth`。
- dev 下 `spring.flyway.enabled=false`；schema 变更新增 `V19__...sql` 或 `R__...sql`，禁止修改已执行脚本。
- 关键表：`sys_user`、`sys_role`、`sys_tenant`、`sys_organization_unit`、`sys_station`、`grant_permission`、
  `tenant_permission`、`sys_feature_value`、`sys_setting`、`audit_log`、`sys_user_login_log`、`sys_serial_no`、
  `sys_user_fingerprint`、`sys_todo*`、`sys_dashboard*`。

## 8. 常见陷阱

1. **`POST /login` 不是 Controller**：登录逻辑在 `common/filter/JWTAuthenticationFilter.java`。
2. **`mapper/` 是 MapStruct**：不是 MyBatis，也没有 XML。
3. **dev 关闭 Flyway**：表结构问题先查是否忘了手工迁移。
4. **缓存双实现**：权限/特性/设置均有 Redis 与内存兜底，改缓存逻辑要同时验证。
5. **Dubbo 序列化检查被禁用**：`serialize-check-status: DISABLE` 是遗留 TODO。
6. **多租户上下文**：必须使用 `TenantContextHolder` / `UserContext`，不要从入参推断。
7. **RPC 变更影响面大**：网关鉴权、workflow 待办、notification 邮件/推送都依赖 auth。
8. **敏感配置**：微信 appid/secret、SM4 密钥、数据库口令不得入库；dev 中的默认值仅用于本地。
9. **行政区划数据**在 `src/main/resources/regions/*.json`，新增地区改数据不要硬编码。
10. **测试数据库风险**：不要对共享环境运行会修改数据的集成测试（例如重置密码的用例）。

## 9. 编码规范

- Java 25、4 空格、UTF-8、`com.dusk.module.auth.*`、Lombok + MapStruct。
- Controller 不直接访问 repository/entity；DTO 转换走 mapper。
- 新增权限点必须挂到权限树，新增接口默认需要鉴权（除非明确匿名）。
- 提交信息遵循 Conventional Commits，例如 `feat(auth): 支持手机验证码登录`。
- 修改 `service.impl` / `common.util` 必须补测试（JaCoCo 门槛）。
