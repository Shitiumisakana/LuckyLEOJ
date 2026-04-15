# LuckyLEOJ 判题系统 (LuckyOJ-backend)

LuckyLEOJ 是一个基于 **Spring Cloud + RabbitMQ + Docker** 构建的工业级在线评测系统（Online Judge）。项目实现了从前端提交、后端调度、消息队列异步解耦到代码沙箱安全隔离的完整链路。

## 🔗 项目组成

* **前端工程**: [LuckyLEOJ-front](https://www.google.com/search?q=https://github.com/Shitiumisakana/LuckyLEOJ-front)
* **后端核心**: [LuckyLEOJ](https://github.com/Shitiumisakana/LuckyLEOJ)
* **代码沙箱**: [LuckyLEOJ-sandbox](https://www.google.com/search?q=https://github.com/Shitiumisakana/LuckyLEOJ-sandbox) (基于 Docker 的安全判题环境)

---

## 🏗️ 系统运行流程图

项目通过微服务架构实现了请求的解耦，核心判题逻辑由 RabbitMQ 异步驱动。

代码段
<img width="1007" height="537" alt="ScreenShot_2026-04-15_154152_505" src="https://github.com/user-attachments/assets/806f26e1-a988-48f6-b633-22613b5dc5f7" />

```
graph TD
    A[用户在前端提交代码] -->|REST| B(Gateway 网关)
    B --> C[Question 微服务]
    C -->|1. 记录入库| D[(MySQL)]
    C -->|2. 发送消息| E{RabbitMQ 交换机}
    E -->|3. 异步推送| F[Judge 判题微服务]
    subgraph "代码沙箱安全环境"
    F -->|4. 接口调用| G[LuckyOJ-Sandbox]
    G -->|5. 编译运行| H[Docker Container]
    H -->|6. 返回执行结果| G
    end
    G -->|7. 返回判题报告| F
    F -->|8. 更新状态与结果| D
    D -.->|9. 前端实时获取结果| A
```

---

## 🚀 核心运行机制说明

### 1\. 异步判题链路 (MQ Decoupling)

为了应对高并发提交，系统引入了 **RabbitMQ**。

* **生产者 (Question Service)**：用户提交后，系统先将记录存入 MySQL 并立即返回“提交成功”给用户，随后将任务投递到消息队列。
* **消费者 (Judge Service)**：判题服务监听队列，按需消费任务。这种设计防止了判题过程（编译、运行）耗时过长导致前端连接阻塞，实现了系统的流量削峰。

### 2\. 代码沙箱与安全隔离 (Sandbox & Docker)

判题的核心在独立的 **Sandbox** 模块中完成，通过 **代理模式** 实现了功能增强：

* **资源限制**：通过 Docker 容器的 `HostConfig` 严格限制 CPU 使用率和内存占用（Memory Limit），防止恶意代码耗尽服务器资源。
* **安全防御**：利用 Docker 容器实现物理级隔离，代码在沙箱内运行无法访问宿主机文件系统或执行高危系统调用。

### 3\. 微服务架构与数据一致性

* 使用 **Nacos** 进行服务发现与配置管理。
* 使用 **Feign** 实现微服务间（如判题服务调用题目服务）的同步通信。
* 针对分布式 ID（雪花算法），后端通过 Jackson 序列化配置将 19 位 **Long** 类型转为 **String** 传给前端，解决 JS 精度丢失问题。

---

## 🛠️ 技术栈选型

| **维度** | **技术选型** | **备注** |
| :--- | :--- | :--- |
| **前端核心** | **React 18, Ant Design Pro, Umi 4** | 企业级中后台前端框架 |
| **前端组件** | **Ant Design, ProComponents** | 高级 UI 组件库，快速构建复杂表单与表格 |
| **编辑器** | **Monaco Editor, Markdown Editor** | 沉浸式代码编辑体验与富文本支持 |
| **网络请求** | **Umi Request (Axios 封装)** | 统一的请求拦截与错误处理 |
| **后端核心** | **Spring Boot 3.x, Java 进程控制** | 核心业务逻辑与沙箱安全控制 |
| **微服务** | **Spring Cloud Gateway, Nacos** | 服务路由、负载均衡与注册中心 |
| **中间件** | **RabbitMQ, Redis** | 消息异步判题、分布式 Session 共享 |
| **存储/持久化** | **MySQL 8.0, MyBatis Plus** | 核心数据存储与 ORM 增强 |
| **安全/沙箱** | **Docker (docker-java), 静态代理** | 环境隔离、资源配额限制、安全逻辑增强 |
| **部署容器** | **Nginx, Docker Compose** | 前端静态托管、网关转发、容器化一键部署 |

---

## 📂 后端项目结构

Plaintext

```
luckyoj-backend
├── luckyoj-backend-gateway           # 微服务网关 (鉴权、路由)
├── luckyoj-backend-common            # 公共工具类、异常处理、常量
├── luckyoj-backend-model             # 实体类、DTO、VO、枚举
├── luckyoj-backend-user-service      # 用户模块 (登录、注册、权限)
├── luckyoj-backend-question-service  # 题目模块 (增删改查、提交)
├── luckyoj-backend-judge-service     # 判题核心模块 (MQ 消费者、策略模式)
├── luckyoj-backend-service-client    # Feign 内部调用接口
└── docker-compose.service.yml        # 容器化部署配置
```

---

## 📋 快速开始

1. **环境配置**：启动 MySQL, Redis, RabbitMQ, Nacos 环境。
2. **SQL 初始化**：执行各模块下的 SQL 脚本完成数据库创建。
3. **配置文件**：修改各微服务的 `application.yml`，填入正确的数据库和中间件地址。
4. **启动顺序**：

1. 启动 `Nacos`
2. 启动 `Gateway`
3. 启动 `User`, `Question`, `Judge` 各微服务模块
5. **前端部署**：将前端工程部署至 Nginx，或通过 `npm run dev` 启动预览。

---

### 💡 开发提示

* 在配置 **阿里云 OSS** 等密钥信息时，请务必使用环境变量或加密处理，避免明文上传至公共仓库。
* 沙箱建议在 Linux 系统运行，以获得更完整的 Docker 资源配额支持。
