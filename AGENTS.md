# AGENTS.md — dusk-module-auth 仓库指南

> 本文件面向 AI 助手与协作者，描述本仓库（`dusk-module-auth`）的功能、结构、约定与常见陷阱。
> 仓库级（工作空间）规范见上级目录 `DuskMS/AGENTS.md`；IDE 辅助说明见 `CODEBUDDY.md`；用户文档见 `README.md`。
> **三者职责不同：** `AGENTS.md` 侧重「改代码前必须知道的事」，`README.md` 侧重「怎么跑起来」，`CODEBUDDY.md` 侧重 IDE 工程辅助。

## 一、模块定位与功能总结

`dusk-module-auth` 是 DuskMS 多租户 SaaS 平台的**认证与授权微服务**，回答"谁来（认证）、能看什么/能做什么（授权）"这一核心访问控制问题。

**核心功能清单：**

| 领域 | 功能 |
| ---- | ---- |
| 登录认证 | 账号密码登录（密码国密 SM4 加密传输）、图形验证码（失败次数触发）、手机验证码登录、扫码登录、微信小程序/企业微信登录、i国网（IGW）单点登录、SSO Token |
| Token 体系 | JWT 签发与校验、永久 Token 签发（`foreverTokenSign`）、`TokenAuthManager` 认证管理、`/token-auth` 接口（authenticate/logout/refresh） |
| 权限体系 | 23 个 `*AuthProvider` 声明 `Pages.*` 权限树常量 → 展平为权限缓存（Redis + 内存兜底）→ URL 级权限映射 + 多租户（Host/Tenant 侧）过滤 + 组织/厂站数据权限过滤 |
| 用户管理 | 用户/外部用户 CRUD、Excel 批量导入导出（easyexcel）、用户指纹（防冒用）、微信账号绑定、密码策略、锁定/解锁、默认厂站设置 |
| 组织与岗位 | 组织单位树、厂站（Station）管理及迁移、组织-角色绑定、组织管理员 |
| 多租户与订阅 | 租户生命周期（连接串/订阅结束日/启用状态）、订阅版本（Edition）、租户可用性校验、功能特性（Feature）开关 |
| 工作台与待办 | Dashboard 首页模块/主题/分类、Todo 待办（含已读/忽略）、消息通知与多端推送（RabbitMQ/阿里云推送） |
| 审计与日志 | 登录日志、审计日志（`UserLogAspect` AOP 自动采集 + 手动记录）、用户指纹日志 |
| 系统设置 | 主机/租户两级设置注册表（邮件、LDAP、安全策略、外观、数据展示等）、行政区划数据（省市区街道）、序列号（SerialNo）生成、系统码（SysCode） |
| 辅助能力 | 角色权限模板 Excel 导入导出、人员 docx 打印模板、姓名转拼音（HanLP）、数据展示集、页面快捷入口 |

## 二、工作空间与模块关系（多仓库）

DuskMS 是**多仓库**工作空间：每个顶层目录是一个独立 Git 仓库，由 `dusk/` Maven 聚合器统一构建。

```
dusk/                     聚合器（pom + docker 部署脚本）
dusk-module-parent/       父 POM
dusk-dependencies/        BOM（外部依赖版本管理）
dusk-common/              core / rpc / doc / mqs 共享构件（GitHub Packages 分发）
dusk-module-auth/         ← 本仓库：认证/用户/权限/租户
dusk-module-ddm/          特性值、系统设置、订阅、动态菜单的最终存储
dusk-module-gateway/      统一入口，鉴权通过 Dubbo RPC 调用本模块
dusk-module-workflow/     工作流引擎
dusk-module-minio/        对象存储
dusk-web/                 Vue3 前端（pnpm monorepo）
```

- 模块间通过 **Dubbo（Nacos 注册中心）** 通信，RPC 接口定义统一放在外部构件 `dusk-common-rpc`。
- 网关鉴权流程：网关收到请求 → 调 `IAuthRpcService.auth(authorization, applicationName, url)`（本模块 `service/impl/AuthRpcServiceImpl` 实现）→ 校验 JWT + URL 权限 → 放行/拒绝。
- 本模块不包含：动态菜单管理（在 ddm）、文件存储（minio）、业务数据。

## 三、构建 / 运行 / 测试

### 前置条件（必须）

- **JDK 21 + Maven 3.9+**（无 `mvnw` wrapper）。
- **GitHub Packages PAT**：内部依赖（`dusk-module-parent`、`dusk-common-*`、`dusk-module-ddm-shared`）从 GitHub Packages 解析，需在 `~/.m2/settings.xml` 配置 `read:packages` 权限的 PAT，server id：`github-dusk-dependencies`、`github-dusk-module-parent`、`github-dusk-common`、`github-dusk-module-ddm`。模板见 `.github/workflows/ci.yml`。
- **无 Maven Central 仓库**：`pom.xml` 未配置 Central 的 `<repositories>`，外部依赖版本统一由 parent 引入的 `dusk-dependencies` BOM 管理，不要自行添加中央仓库依赖版本。
- **运行时依赖**：Nacos（配置+注册中心，配置导入 `optional:nacos:dusk-module-auth.yaml`）、PostgreSQL（Druid 池）、Redis（权限/特性/设置缓存）。RabbitMQ / RocketMQ / EMQX 可选（`SPRING_RABBITMQ_ISENABLED` 控制）。

### 命令

```bash
# 一键启动中间件栈（Postgres/Redis/Nacos/...）
cd dusk/docker && docker compose up -d

# 单模块构建（跳过测试）
mvn -B -ntp -U -DskipTests clean package

# 全工作空间聚合构建（推荐）
cd DuskMS/dusk && mvn clean install -DskipTests

# 本地运行（默认 profile=sit，开发用 dev；端口 53201）
cd DuskMS/dusk-module-auth
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 或 SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# 测试（生成 JaCoCo 报告）
bash run-tests.sh
mvn test -Dtest=CaptchaServiceImplTest
mvn test -Dtest=CaptchaServiceImplTest#testGetCaptcha
```

### 关键配置（环境变量覆盖）

| 项 | 环境变量 | dev 默认值 |
| ---- | ---- | ---- |
| 端口 | `SERVER_PORT` | `53201` |
| 数据源 | `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | `jdbc:postgresql://dusk.com:5432/dusk` |
| Nacos | `SPRING_CLOUD_NACOS_*` | `dusk.com:8848`，namespace `dev` |
| 匿名路径 | `app.security.ignores` | `/login`、`/error/**`、swagger 等 |
| 登录密码密钥 | `app.login.encrypt-key` | SM4 16 进制密钥 |
| 微信测试模式 | `WX_TEST_ENABLED` | `true`（不存在手机号落到 `test` 账号） |

> dev profile 下 `flyway.enabled: false`，需要手动执行迁移脚本或依赖已有库。

## 四、代码结构与分层

```
src/main/java/com/dusk/module/auth/
├── controller/         REST 控制器（27 个），注意 POST /login 不在其中（见"登录鉴权"）
├── service/            Service 接口 + Dubbo RPC 接口（IFeatureRpcService 在本地定义）
├── service/impl/       Service 实现 + @DubboService 对外 RPC 实现
├── repository/         Spring Data JPA（继承 dusk-common-core 的 IBaseRepository）
├── entity/             JPA 实体（User/Role/Tenant/Station/OrganizationUnit/... 27 个）
├── entity/dashboard/   首页模块/主题/分类实体（7 个）
├── dto/                约 30 个子包（按领域分：user/role/tenant/setting/...）
├── enums/              枚举
├── common/             安全、权限缓存、工具（见下）
├── authorization/      权限树 Provider（23 个）
├── feature/ + impl/    功能特性开关子系统
├── setting/            设置注册表（主机/租户两级）
├── syscode/            系统码定义 + 组织树修复
├── cache/              特性/指纹/微信关系缓存（Redis + 内存兜底）
├── push/               通知推送抽象（RabbitMQ 本地实现）
├── aspect/             UserLogAspect / WxLoginAspect（AOP 审计；/login 绕过 aspect，日志在 JWTAuthenticationFilter 手动记录）
├── igw/                i国网单点登录（AppSSOLoginServiceImpl）
├── excel/              Excel 导入监听器/导出器（listener/ExcelDataListener、LogInOutEvent）
├── listener/           RabbitMQ 等消息监听
├── utils/              okhttp 等工具
└── DuskAuthApplication.java   启动类（扫描 com.dusk.module.auth + common-core + common-rpc）
```

**分层规则：** `controller → service（接口）/impl → repository → PostgreSQL`。补充：
- `mapper/` 是 **MapStruct DTO 转换器**（`XxxMapper.INSTANCE`），**不是 MyBatis，没有 XML**。
- `manage/` 放 `@Transactional` 跨实体操作（如 `UserManage` 的角色/权限查询）。
- 复杂查询用 **QueryDSL**（`JPAQueryFactory` + target/generated-sources 生成的 `Q*` 类）。
- 实体用 `@EntityGraph` 控制抓取策略（如 `User.role`）。

## 五、登录鉴权流程（最重要）

```
1. 前端 POST /login（账号密码 + 验证码，密码 SM4 加密传输）
2. JWTAuthenticationFilter（common/filter/，非 Controller）→ 校验验证码
   → DefaultAuthenticationProvider 校验凭证
3. 成功：签发 JWT 随响应返回；同时记录登录/审计日志（success/failure handler）
4. 后续请求携带 JWT，由网关调用 AuthRpcService.auth(authorization, app, url) 鉴权
5. TokenAuthManager 校验 Token → DefaultInvocationSecurityMetadataSource 解析 URL 权限
   → DefaultAccessDecisionManager 决策放行/拒绝
6. 登录态经 RpcContext/请求头传递，业务层通过 UserContext（dusk-common-core）取当前用户
```

安全相关类全部在 `common/` 下：
- `config/SecurityConfiguration.java`（过滤器链 + ProviderManager + 密码编码器）
- `filter/JWTAuthenticationFilter.java`、`handler/DefaultAuthenticationSuccess/FailureHandler`
- `provider/DefaultAuthenticationProvider`、`CustomAuthProvider`（喂跨应用 URL/权限信息）
- `manage/TokenAuthManager`、`DefaultAccessDecisionManager`
- `metadata/DefaultInvocationSecurityMetadataSource`、`skiprequest/SkipPathRequestMatcher`
- `datafilter/DataFilterDefinitionContext`（组织父子关系 → 数据权限范围）
- `permission/`（AuthPermissionManager、RedisPermissionCache/DefaultPermissionCache）

**注意：所有请求默认 `permitAll`，真正的强制发生在 RPC 边界（网关调用 auth 时）。**

## 六、权限 / 特性 / 设置子系统

### 权限树（authorization/）
- `AdministrationAuthProvider` 是根（`Pages.Administration`），其余 Provider 用 `createChildPermission("Pages.Xxx", 名称, MultiTenancySides.Host/Tenant)` 声明权限常量。
- 启动时被展平进权限缓存（`RedisPermissionCache`，内存 `DefaultPermissionCache` 兜底），`AuthPermissionManager` 负责租户/Host 过滤。
- 常用：`RoleAuthProvider`、`TenantAuthProvider`、`UserLoginLogAuthProvider`、`StationAuthProvider`、`SerialNoAuthProvider`、`HostSettingsAuthProvider` 等，共 23 个。
- **新增一个页面/按钮权限 = 新增/修改一个 `*AuthProvider` 常量**，无需建表（权限常量树 + `grant_permission`/`tenant_permission` 表落库授权关系）。

### 特性开关（feature/ + impl/）
- `*FeatureProvider`（8 个：Login/User/Menu/Todo/DashBoard/AppNotification/ThirdParty/CenterControl）声明特性 key + 默认值 + UI widget 类型。
- `impl/FeatureManager` 启动收集定义并推送到 `FeatureRpcServiceImpl`（**RPC 实现在本模块**，`IFeatureRpcService` 接口也在本地 `service/`），存 Redis（`IFeatureCache`）。
- `impl/FeatureChecker` 通过自引用 Dubbo 读租户特性值，常用 `isEnabled("...")` / `getValue("...")`。

### 设置注册表（setting/）
- `*SettingProvider`（HostSettingProvider/EmailSettingProvider/ApplicationSettingProvider 等）注册设置项，`SettingManagerDefault` 管理，`RedisSettingsCache`/`DefaultSettingsCache` 缓存，`MainSettingsPublish` 发布。
- `setting/config/` 下有 LdapModuleConfig、MultiTenancyConfig、TicketManagementConfig、PushManagementConfig。

## 七、对外 Dubbo RPC（服务端，本模块实现）

接口定义绝大多数在外部构件 `dusk-common-rpc`（如 `com.dusk.common.rpc.auth.service.IAuthRpcService`），本模块 `service/impl/` 用 `@DubboService` 实现：

`AuthRpcServiceImpl`、`UserRpcServiceImpl`、`RoleServiceImpl`、`TenantRpcServiceImpl`、`SerialNoServiceImpl`、`StationRpcService`、`TodoRpcServiceImpl`、`AuditLogRpcServiceImpl`、`PushRpcServiceImpl`、`TokenAuthRpcService`、`UserFingerprintRpcServiceImpl`、`CustomerRpcServiceImpl`、`EmailServiceImpl`、`NotificationServiceImpl`、`OrganizationUnitServiceImpl`、`SettingServiceImpl`、`CommonFavoriteServiceImpl`、`FeatureRpcServiceImpl`。

> 改 RPC 接口（方法签名/出入参）会**影响网关与其他模块**：若接口在 dusk-common-rpc 中定义，需同步修改并发布该构件；`IFeatureRpcService` 例外，接口在本地。

**消费侧（`@DubboReference`）**：本模块也会引用远程服务，注意区分来源——
- 引用**本模块自己注册**的 RPC：如 `IFeatureRpcService`（FeatureChecker/FeaturePusher 自引用）、`IUserRpcService`/`IRoleRpcService`（DashBoardServiceImpl 使用）。
- 引用**其他模块**的 RPC：如 `ISettingRpcService`、`IDynamicMenuRpcService` 来自 `com.dusk.module.ddm.*`（ddm 模块），`ISerialNoRpcService` 来自 `com.dusk.common.rpc.auth.*`。
- Provider 由 `dubbo.scan.base-packages: com.dusk.module.auth.service` 扫描注册；注册中心为 Nacos。

## 八、数据库与 Flyway

- 迁移脚本 `src/main/resources/db/migration/`：**V1~V18**（18 个版本脚本）+ **R__ 约 35 个可重复脚本**，历史表 `flyway_schema_history_auth`（pom 中 `flyway.table` 指定）。
- dev 配置关闭 Flyway；schema 变更必须新增 `V19__...sql`（或可重复 `R__`），不要改已执行的历史脚本。
- 关键表：`sys_user`、`sys_role`、`sys_tenant`、`sys_station`、`sys_organization_unit`、`grant_permission`、`tenant_permission`、`sys_feature_value`、`sys_setting`、`audit_log`、`sys_user_login_log`、`sys_serial_no`、`sys_user_fingerprint`、`sys_user_wx_relation`、`sys_todo*`、`sys_dashboard*` 等。
- 无 Menu/Permission 实体表：菜单/权限是常量树 + 授权关系表，动态菜单数据在 ddm 模块。

## 九、测试现状（重要，别被文档误导）

- **当前只有一个测试类**：`src/test/java/.../CruxModuleAuthApplicationTests.java`（`@SpringBootTest`，需要真实 DB/Redis/Nacos）。
- README/CODEBUDDY 提到的 68 用例套件（`BaseUnitTest`、`TestDataBuilder`、`CaptchaServiceImplTest` 等）**已被删除**（commit `3984c89`），文档保留仅作设计参考，不要假设它们存在。
- `pom.xml` 有 **JaCoCo 硬性门槛**：`com.dusk.module.auth.service.impl` 与 `common.util` 分支覆盖率 ≥ 60%、行 ≥ 70%，`mvn test` 时未达标会**构建失败**——在这两个包改代码必须补测试。
- ⚠️ `CruxModuleAuthApplicationTests#resetUserPassword()` 会**改写真实用户密码**，绝不要对共享/测试环境以外的数据运行。

## 十、常见陷阱与注意事项

1. **构建失败先查 PAT**：GitHub Packages 私有依赖解析失败 = `~/.m2/settings.xml` 未配置或 token 过期。
2. **`POST /login` 不是 Controller**：找登录逻辑去 `common/filter/JWTAuthenticationFilter.java`。
3. **`mapper/` 不是 MyBatis**：是 MapStruct，搜 SQL 不要搜这个目录。
4. **Flyway 在 dev 下关闭**：本地改表需手动执行 SQL 或开启 flyway。
5. **Dubbo 序列化安全检查当前 DISABLE**（application.yml 有 TODO），生产加固前不要随便启用。
6. **多租户上下文**：`TenantContextHolder`/`UserContext` 来自 dusk-common-core，业务代码里取当前用户/租户必须通过它们，不要自行从参数推断。
7. **缓存在 Redis，有内存兜底**：改权限/特性/设置相关逻辑时，两个实现都要测。
8. **Secret 不要提交**：微信 appid/secret、SM4 密钥、DB 密码等一律走环境变量/Nacos。
9. **RPC 变更影响面大**：改 `*RpcServiceImpl` 前确认网关（`dusk-module-gateway`）与 ddm 等消费方兼容。
10. **行政区划数据**在 `src/main/resources/regions/*.json`，新增地区要同步数据而非硬编码。

## 十一、编码规范

- Java 21、4 空格缩进、UTF-8、`com.dusk.module.auth.*` 包名、Lombok/MapStruct。
- 分层调用：controller 不直接碰 repository/entity，DTO 转换走 mapper。
- 提交信息用 Conventional Commits：`feat(auth): 支持手机验证码登录`（类型：feat/fix/perf/style/docs/test/refactor/build/ci/chore/revert）。
- 新功能补测试（尤其 `service.impl`、`common.util` 两个被 JaCoCo 卡住的包）；提交前 `mvn test` 通过。
