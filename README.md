# dusk-module-auth 认证授权服务

> DuskMS 平台的认证与授权微服务，为多租户 SaaS 系统提供统一的登录认证、权限控制、用户/组织/租户管理与订阅功能特性控制能力，解决"谁来、能看什么、能做什么"的核心访问控制问题。

## 技术栈

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.12-brightgreen)
![Apache Dubbo](https://img.shields.io/badge/Dubbo-3.2.8-blueviolet)
![Maven](https://img.shields.io/badge/Maven-3.9+-yellow)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Druid%20Pool-336791)
![Redis](https://img.shields.io/badge/Cache-Redis-red)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)
[![CI](https://img.shields.io/badge/CI-GitHub%20Actions-blue)](.github/workflows/ci.yml)

| 维度 | 说明                                                                   |
| ---- |------------------------------------------------------------------------|
| 版本 | 1.1.0-SNAPSHOT                                                         |
| 语言/框架 | Java 21 · Spring Boot 3.2 · Spring Security · Spring Data JPA          |
| RPC | Apache Dubbo 3.2.8（Nacos 注册/配置中心）                              |
| 数据存储 | PostgreSQL（Druid 连接池）· Redis 缓存 · Flyway 迁移                   |
| 其他 | QueryDSL · MapStruct · Fesod · 微信小程序/企业微信 · springdoc-openapi |

## 核心功能

### 已实现

- **登录认证**：账号密码登录（国密 SM4 加密）、图形验证码（失败次数触发）、手机验证码登录、扫码登录、微信小程序/企业微信登录、i国网（IGW）单点登录
- **Token 体系**：JWT 签发与校验、永久 Token 签发（`foreverTokenSign`）、Token 认证管理
- **权限体系**：基于 `AuthorizationProvider` 的权限树定义（`Pages.*`）、URL 级权限映射、多租户（租户侧/主机侧）权限过滤、组织/厂站数据权限过滤
- **用户管理**：用户、外部用户、Excel 批量导入导出、人员打印、用户指纹、微信账号绑定、密码策略
- **组织与岗位**：组织单位树、厂站（Station）管理及迁移、组织-角色绑定
- **多租户与订阅**：租户生命周期管理、租户可用性校验、订阅版本（Edition）、功能特性（Feature）开关
- **工作台与待办**：Dashboard 首页模块/主题/分类、Todo 待办、消息通知与多端推送
- **审计与日志**：登录日志、审计日志（AOP 自动采集，`/login` 手动记录）、操作追踪
- **系统设置**：主机/租户两级设置（邮件、LDAP、安全策略、外观、数据展示等）、行政区划数据、序列号（SerialNo）生成
- **监控与集成**：`/actuator` 端点、OpenTelemetry 上报、RocketMQ/MQTT 消息

### 规划中 (Todo)

- [ ] User / Role / GrantPermission 等核心服务完整单元测试覆盖（目标全模块分支覆盖率 ≥ 80%，详见 `UNIT_TEST_GUIDE.md`）
- [ ] 补充自定义 Dubbo 序列化安全白名单（当前 `serialize-check-status: DISABLE`，见 `application.yml`）
- [ ] 集成测试套件（Testcontainers）与 CI 覆盖率门槛
- [ ] 基于ABAC的数据维度控制

## 架构与模块设计

### 微服务划分（DuskMS 工作空间）

```
                    ┌─────────────────────────────────────────┐
   Vue3 前端        │  dusk-module-gateway（统一入口/鉴权转发） │
                    └───────────────┬─────────────────────────┘
                                    │ Dubbo RPC (鉴权/用户/角色/特性...)
        ┌───────────────────────────┼──────────────────────────────┐
        ▼                           ▼                              ▼
 dusk-module-auth            dusk-module-ddm                 dusk-module-workflow
 认证/用户/权限/租户          特性/设置/订阅/动态菜单              工作流引擎
        │                           │                              │
        └──────────┬────────────────┴──────────────┬───────────────┘
                   ▼                               ▼
          dusk-common (core/rpc/doc/mqs)      PostgreSQL · Redis · RocketMQ · EMQX · MinIO
```

- **dusk-module-auth**（本模块）：登录认证、权限定义与校验、用户/组织/角色/租户/订阅等身份域管理
- **dusk-module-ddm**：功能特性值、系统设置、订阅版本等运行时配置的最终存储
- **dusk-common**：`core`（JPA/安全/JWT/多租户基础设施）、`rpc`（跨模块 Dubbo 接口定义）、`doc`、`mqs`
- 模块间通过 Dubbo（Nacos 注册中心）通信，RPC 接口统一在 `dusk-common-rpc` 中定义

### 模块内部分层

```
controller ──> service / service.impl ──> repository (Spring Data JPA) ──> PostgreSQL
                    │         │
                    ├── mapper/（MapStruct DTO 转换，非 MyBatis）
                    ├── manage/（@Transactional 跨实体操作，如 UserManage）
                    ├── QueryDSL（JPAQueryFactory + Q* 类，复杂查询）
                    ├── Redis 缓存（权限/特性/设置/指纹等，内存实现为降级兜底）
                    ├── authorization/（AuthorizationProvider 权限树定义）
                    ├── feature/ + impl/（特性定义与特性开关）
                    └── setting/（设置注册表与 Redis 缓存）
```

### 登录鉴权流程

```
1. 前端 POST /login（账号密码 + 验证码，密码 SM4 加密传输）
2. JWTAuthenticationFilter：校验验证码 → DefaultAuthenticationProvider 校验凭证
3. 成功后签发 JWT，随响应返回；同时记录登录/审计日志
4. 后续请求携带 JWT：网关调用 AuthRpcService.auth(authorization, app, url)
5. TokenAuthManager 校验 Token → MetadataSource 解析 URL 权限
   → AccessDecisionManager 决策，放行或拒绝
6. 登录态经 RpcContext/请求头传递，业务层通过 UserContext 获取当前用户
```

## 快速入门

### 1. 环境准备 (Prerequisites)

| 依赖 | 版本/说明 |
| ---- | ---- |
| JDK | 21 |
| Maven | 3.9+（无 mvnw wrapper） |
| PostgreSQL | 数据库（Druid 连接池） |
| Redis | 缓存（权限/特性/设置） |
| Nacos | 注册中心 + 配置中心（必须） |
| RocketMQ / EMQX | 可选，消息推送与 MQTT |

内部依赖（`dusk-module-parent`、`dusk-common-*`、`dusk-module-ddm-shared`）托管于 GitHub Packages 私有仓库，构建前需在 `~/.m2/settings.xml` 配置具有 `read:packages` 权限的 GitHub PAT（server id：`github-dusk-dependencies`、`github-dusk-module-parent`、`github-dusk-common`、`github-dusk-module-ddm`）。模板参考 `.github/workflows/ci.yml`。

一键启动中间件：

```bash
cd dusk/docker && docker compose up -d   # Postgres/Redis/Nacos/RocketMQ/EMQX/MinIO 及可观测组件
```

### 2. 配置文件修改

主要配置位于 `src/main/resources/application-{dev,sit,prod}.yml`，通过环境变量注入敏感信息（推荐用环境变量覆盖，勿提交明文密钥）：

| 参数 | 环境变量 | 说明 |
| ---- | ---- | ---- |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | 默认 `sit`，本地开发用 `dev` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | PostgreSQL 连接串（dev 默认 `jdbc:postgresql://dusk.com:5432/dusk`） |
| `spring.datasource.username/password` | `SPRING_DATASOURCE_USERNAME/PASSWORD` | 数据库账号密码 |
| `server.port` | `SERVER_PORT` | 默认 `53201` |
| `spring.cloud.nacos.server-addr` | `SPRING_APPLICATION_NAME` 等 | Nacos 地址（dev 默认 `dusk.com:8848`） |
| `spring.config.import` | — | 从 Nacos 拉取 `dusk-module-auth.yaml` 覆盖配置 |
| `app.login.encrypt-key` | — | 登录密码 SM4 解密密钥（16 进制） |
| `app.security.ignores` | — | 匿名访问路径白名单 |

> dev 配置中 `flyway.enabled: false`；迁移脚本位于 `src/main/resources/db/migration`，历史表 `flyway_schema_history_auth`。

### 3. 启动运行

```bash
# 克隆仓库
git clone https://github.com/MouckIS/dusk-module-auth.git
cd dusk-module-auth

# 方式一：本模块单独构建（需已配置 GitHub Packages PAT）
mvn -B -ntp -U -DskipTests clean package

# 方式二：从工作空间聚合构建（推荐，含父 POM 与公共依赖）
mvn -f ../dusk/pom.xml clean install -DskipTests

# 运行（默认加载 sit 配置）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 或 SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

## 生产与容器化部署

- **中间件**：使用 https://github.com/MouckIS/dusk 项目的`/docker/docker-compose.yaml` 一键拉起 Postgres、Redis、Nacos、RocketMQ、EMQX、MinIO 及 Prometheus/Grafana/Tempo 可观测组件。
- **服务部署**：仓库内置 `Dockerfile`（多阶段构建，以非 root 用户运行），内部依赖托管于 GitHub Packages

  ```bash
  cd dusk-module-auth
  docker build -t dusk-module-auth .
  docker run -d -p 53201:53201 \
      -e SPRING_PROFILES_ACTIVE=sit \
      -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/dusk \
      -e SPRING_DATASOURCE_USERNAME=<user> \
      -e SPRING_DATASOURCE_PASSWORD=<password> \
      dusk-module-auth
  ```

  镜像默认暴露 `8080` 端口，默认 profile 为 `sit`；请通过环境变量注入真实中间件地址与密钥。若本机已配置好 `~/.m2/settings.xml`，也可挂载该文件跳过 `--build-arg`。
- **线上地址**：暂无公开演示环境。

## 开发者指引

### 接口文档

项目集成 [springdoc-openapi](https://springdoc.org/)，启动后访问：

- Swagger UI：`http://localhost:53201/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:53201/v3/api-docs`

### 测试

```bash
bash run-tests.sh            # 编译 + 全部测试 + 生成 JaCoCo 覆盖率报告
mvn test -Dtest=UserServiceImplTest            # 运行单个测试类
mvn test -Dtest=UserServiceImplTest#testMethod # 运行单个测试方法
```

测试框架：JUnit 5 + Mockito + AssertJ。`pom.xml` 内置 JaCoCo 覆盖率门槛（`service.impl`、`common.util` 分支 ≥ 60%、行 ≥ 70%），未达标的改动会导致构建失败。单元测试框架与最佳实践详见 `UNIT_TEST_GUIDE.md`，项目成果总结见 `TEST_SUMMARY.md`。

### 贡献指南 (Contributing)

当前仓库暂无 `CONTRIBUTING.md`，请遵循以下约定：

1. 先阅读仓库根目录 `AGENTS.md`（工作空间规范）与本文件；如有疑问请先开 Issue 讨论
2. 提交信息使用 Conventional Commits：`type(scope): subject`（如 `feat(auth): 支持手机验证码登录`），说明改动内容与原因
3. 提交 PR 前确保 `mvn test` 通过、Java 21 编译无告警，并附上相关变更说明（UI 改动附截图）
4. CI（`.github/workflows/ci.yml`）会在 PR 上自动构建校验

## License

本项目基于 [Apache License 2.0](LICENSE) 开源。
