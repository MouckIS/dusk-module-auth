# dusk-module-auth

DuskMS 的 **认证与授权服务**：负责登录认证、JWT 签发与校验、用户/角色/租户/组织管理、权限树、功能特性开关、系统设置、待办与审计日志。

- 聚合坐标：`dusk-module:dusk-module-auth-group:1.2.0`
- 服务坐标：`com.dusk:dusk-module-auth:1.2.0`
- 契约坐标：`com.dusk:dusk-module-auth-shared:1.2.0`
- 端口：dev/sit `53201`

## 核心能力

| 领域        | 功能                                                                                                  |
|-------------|-------------------------------------------------------------------------------------------------------|
| 登录认证    | 账号密码（SM4 加密传输）、图形验证码、手机验证码、扫码登录、微信小程序/企业微信、i国网 SSO、SSO Token |
| Token       | JWT 签发/校验、永久 Token、`/token-auth` 认证/登出/刷新                                               |
| 权限        | 权限树常量（`Pages.*`）→ 权限缓存（Redis + 内存兜底）→ URL 权限映射 → 租户/组织数据权限               |
| 用户        | 用户/外部用户 CRUD、Excel 导入导出、密码策略、锁定/解锁、指纹、微信绑定、默认厂站                     |
| 组织        | 组织单位树、厂站管理、组织-角色绑定、组织管理员                                                       |
| 租户        | 租户生命周期、订阅版本、功能特性开关、租户可用性校验                                                  |
| 工作台/待办 | Dashboard、Todo、消息与推送                                                                           |
| 审计        | 登录日志、审计日志（AOP 自动采集）、用户指纹日志                                                      |
| 设置        | Host / Tenant 两级设置注册表、行政区划、序列号、系统码                                                |

## 技术栈

Java 25 · Spring Boot 4.0.8 · Spring Security · Spring Data JPA · QueryDSL · MapStruct · Dubbo 3.3.6 · Nacos ·
PostgreSQL · Redis · Flyway · EasyExcel · HanLP · EasyCaptcha · BouncyCastle（国密）· 微信小程序 SDK · JJWT。

## 模块结构

```text
dusk-module-auth/
├── dusk-module-auth/            可运行的认证服务
│   └── src/main/java/com/dusk/module/auth/
│       ├── controller/          REST 接口（约 27 个）
│       ├── service/ + impl/     业务服务 + @DubboService 实现
│       ├── repository/          Spring Data JPA
│       ├── entity/              JPA 实体（用户/角色/租户/组织/厂站/待办/审计…）
│       ├── mapper/              MapStruct（不是 MyBatis）
│       ├── common/              Security 过滤器链、权限缓存、数据权限、工具
│       ├── authorization/       权限树 Provider
│       ├── feature/ + impl/     功能特性开关
│       ├── setting/             设置注册表
│       ├── syscode/             系统码定义
│       ├── cache/               Redis + 内存兜底缓存
│       ├── aspect/ audit 切面
│       ├── excel/               导入导出监听器/导出器
│       ├── igw/                 i国网 SSO
│       └── utils/
└── dusk-module-auth-shared/     跨模块 RPC 契约（70 个类：I*RpcService + DTO）
```

## 登录与鉴权链路

```text
POST /login（账号 + 密码 + 验证码）
  → JWTAuthenticationFilter（common/filter/，不是 Controller）
  → DefaultAuthenticationProvider 校验凭证
  → 成功签发 JWT，记录登录/审计日志

后续请求
  → 网关 AccessGatewayFilter 调用 IAuthRpcService.auth(authorization, applicationName, url)
  → TokenAuthManager 校验 Token
  → DefaultInvocationSecurityMetadataSource 解析 URL 权限
  → DefaultAccessDecisionManager 决策放行/拒绝
  → 业务服务通过 UserContext / TenantContextHolder 获取上下文
```

> 注意：`POST /login` 不在 Controller 中，排查登录问题请直接看 `common/filter/JWTAuthenticationFilter.java`。

## 快速开始

### 前置条件

- JDK 25、Maven 3.9+
- GitHub Packages PAT（`read:packages`）
- PostgreSQL、Redis、Nacos

### 构建与运行

```bash
export GH_PAT=<your-pat>

# 构建（模块自带 settings.xml）
mvn -B -ntp -U -s settings.xml -DskipTests clean package

# 本地运行（默认 profile=sit，开发用 dev）
mvn -s settings.xml spring-boot:run -Dspring-boot.run.profiles=dev

# 测试（含 JaCoCo 覆盖率检查）
mvn -s settings.xml test
```

### 关键配置

| 项           | 环境变量                                  | dev 默认值                             |
|--------------|-------------------------------------------|----------------------------------------|
| 端口         | `SERVER_PORT`                             | 53201                                  |
| 数据源       | `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | `jdbc:postgresql://dusk.com:5432/dusk` |
| Nacos        | `SPRING_CLOUD_NACOS_*`                    | `dusk.com:8848`，namespace `dev`       |
| 匿名路径     | `app.security.ignores`                    | `/login`、`/error/**`、swagger         |
| 登录密码密钥 | `app.login.encrypt-key`                   | SM4 16 进制密钥                        |
| 微信测试模式 | `WX_TEST_ENABLED`                         | `true`                                 |
| RabbitMQ     | `SPRING_RABBITMQ_ISENABLED`               | `false`                                |

> dev profile 下 `spring.flyway.enabled=false`，本地改表需手动执行 SQL 或临时开启 Flyway。

## 数据库

- 迁移脚本：`dusk-module-auth/src/main/resources/db/migration/`（`V1`~`V18` 版本脚本 + `R__` 可重复脚本）。
- 历史表：`flyway_schema_history_auth`。
- 主要表：`sys_user`、`sys_role`、`sys_tenant`、`sys_organization_unit`、`sys_station`、`grant_permission`、
  `tenant_permission`、`sys_feature_value`、`sys_setting`、`audit_log`、`sys_user_login_log`、`sys_serial_no`、
  `sys_user_fingerprint`、`sys_todo*`、`sys_dashboard*`。
- 菜单与权限为 **常量树 + 授权关系表**，没有独立的权限实体表；动态菜单数据在 metadata 服务。

## 对外 RPC

`dusk-module-auth-shared` 中的 `I*RpcService` 由本服务 `service/impl` 下的 `@DubboService` 实现，主要接口：

`IAuthRpcService`、`IUserRpcService`、`IRoleRpcService`、`ITenantRpcService`、`IOrganizationUnitRpcService`、
`IStationRpcService`、`ITodoRpcService`、`IAuditLogRpcService`、`ITokenAuthRpcService`、`IUserFingerprintRpcService`、
`ICustomerRpcService`、`ICommonFavoriteRpcService`、`ISsoTokenRpcService`、`ISysMenuRpcService`、`IWxMaRpcService`、
`ISmRpcUtil`。

Dubbo 扫描包：`com.dusk.module.auth.service`。

## 测试

- 44 个测试类（`src/test/java`），以 Mockito 单元测试为主。
- `CruxModuleAuthApplicationTests` 在 surefire 中被排除（需要真实 PostgreSQL/Redis/Nacos）。
- JaCoCo 门槛：`com.dusk.module.auth.service.impl` 与 `common.util` 包分支覆盖率 ≥ 60%、行覆盖率 ≥ 70%，不达标构建失败。

## 相关文档

- [`docs/登录认证授权完整流程.md`](../docs/登录认证授权完整流程.md)
- [`docs/权限优化方案-整理版.md`](../docs/权限优化方案-整理版.md)
- [`docs/数据维度控制设计实现方案.md`](../docs/数据维度控制设计实现方案.md)

## 许可证

[Apache License 2.0](LICENSE)
