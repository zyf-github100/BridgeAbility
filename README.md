# BridgeAbility / 融职桥

面向残障大学生与青年求职者的无障碍就业协同平台。

仓库当前公开的是项目核心源码与构建配置，包含前端应用、后端服务、数据库结构和基础部署文件；本地比赛材料、运行数据、上传文件、批量初始化数据和含敏感信息的部署脚本未纳入公开仓库。

## 项目概览

项目围绕四类角色展开：

- 求职者：个人档案、能力展示、辅助需求、简历预览、投递记录、面试辅助、服务记录
- 企业：岗位发布、候选人管理、面试流程、招聘统计、企业认证
- 服务机构：个案管理、干预记录、资源转介、跟进管理、预警看板
- 管理端：用户管理、审核、风险记录、标签字典、知识库、通知与匹配配置

后端已按业务域拆分为 `auth`、`jobseeker`、`enterprise`、`serviceorg`、`matching`、`notification`、`knowledge`、`admin`、`audit` 等模块，前端对应提供多角色视图页面与接口封装。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router
- 后端：Java 17、Spring Boot 3.1、Spring Security、MyBatis、JWT
- 数据与基础设施：MySQL 8、Redis、Java Mail、S3/R2 兼容对象存储
- 文档导出：Apache POI、OpenPDF

## 仓库结构

```text
.
|-- frontend/     Vue 3 前端应用
|-- backend/      Spring Boot 后端服务
|-- prototype/    早期原型页面
`-- README.md
```

关键目录说明：

- `frontend/src/views/`：多角色业务页面
- `frontend/src/api/`：前端接口封装
- `backend/src/main/java/com/rongzhiqiao/`：后端业务代码
- `backend/src/main/resources/schema.sql`：数据库表结构
- `backend/deploy/backend.env.example`：部署环境变量示例

## 本地启动

### 1. 准备依赖

- Java 17
- Maven
- Node.js 与 npm
- MySQL 8
- Redis

### 2. 启动后端

在 `backend/` 目录执行：

```bash
mvn spring-boot:run
```

默认配置要点：

- 服务端口：`8081`
- 数据库：`jdbc:mysql://localhost:3306/rongzhiqiao`
- Redis：`localhost:6379`

可通过环境变量覆盖：

```bash
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
SPRING_DATA_REDIS_PASSWORD
SPRING_DATA_REDIS_DATABASE
JWT_SECRET
APP_PRIVACY_ENCRYPTION_SECRET
```

邮件配置通过可选文件加载：

```text
backend/config/application-mail.yml
```

健康检查接口：

```text
GET /api/system/health
```

### 3. 启动前端

在 `frontend/` 目录执行：

```bash
npm install
npm run dev
```

默认开发地址：

```text
http://127.0.0.1:4173
```

开发环境下，前端会将 `/api` 代理到：

```text
http://localhost:8081
```

## Docker 运行

后端目录已提供 `docker-compose.yml`，需要先设置这些环境变量：

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `JWT_SECRET`
- `APP_PRIVACY_ENCRYPTION_SECRET`

前端目录也提供了独立 `docker-compose.yml`，需要设置：

- `BACKEND_ORIGIN`

## 公开仓库说明

出于隐私与安全考虑，以下内容未公开：

- 批量初始化业务数据 `backend/src/main/resources/data.sql`
- 上传材料、运行日志、构建产物、`node_modules`
- 比赛提交包、说明文档、报名材料
- 含服务器地址和明文密码的本地部署辅助脚本

如果你要在本地复现：

- 用 `backend/src/main/resources/schema.sql` 建表
- 参考 `backend/deploy/backend.env.example` 补齐环境变量
- 自行准备测试数据、邮件配置和对象存储配置

## 后续建议

- 补充接口文档或 OpenAPI 文档
- 增加根目录一键启动脚本
- 为公开仓库补充示例数据生成脚本，替代本地 `data.sql`
- 补充 CI、测试说明与部署流程图
