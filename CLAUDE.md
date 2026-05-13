# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 构建与测试命令

```bash
# 构建整个项目
mvn clean compile

# 运行全部模块的测试
mvn clean test

# 构建单个模块（跳过测试）
mvn clean install -pl <module> -DskipTests

# 运行单个测试类
mvn test -pl <module> -Dtest=<TestClass>

# 运行单个测试方法
mvn test -pl <module> -Dtest=<TestClass>#<method>
```

Java 17，Spring Boot 3.2.5，Spring Cloud 2023.0.1，Spring Cloud Alibaba 2023.0.1.0。

## 架构概览

**高并发事件中台**（目标 8000+ QPS），事件驱动、Kafka 解耦的微服务架构。

### 核心数据流

```
HTTP/MQTT → ingest-service → Kafka → gateway-service（API 网关）
                                         → consumer-service → Kafka → storage-service → MySQL
                                         → alert-service（骨架）
                                         → query-service（骨架）
                                         → stream-service（骨架）
                                         → ai-analysis-service（RAG 智能运维助手）
```

### 模块一览

| 模块 | 职责 | 关键依赖 |
|---|---|---|
| `common-lib` | 共享领域模型：`Event`、`Result<T>`、`CameraMessage`、`FailedEventMessage`、`CameraFailureMessage` | Lombok |
| `ingest-service` | 双通道入口：HTTP POST `/event/report` 和 MQTT（事件通道 + 摄像头通道各一条）。事件标准化后投递 Kafka。 | Spring Kafka, Spring Integration MQTT |
| `gateway-service` | Spring Cloud Gateway，全局认证过滤器（JWT），IP/设备黑名单校验，Redis 限流。公开路径 `/api/auth/login`、`/api/ingest/**` 跳过鉴权。 | Spring Cloud Gateway, Redis, jjwt |
| `consumer-service` | `@KafkaListener` 消费 `event-topic`，通过 `TaskExecutor` 异步处理，再投递到存储 Topic。线程池饱和时发布失败消息。 | Spring Kafka |
| `storage-service` | 批量消费 `event-storage-topic`，经 MyBatis-Plus 写入 MySQL（`event_archive` 表）。Sentinel 熔断 + 可配置重试。含事件和摄像头两条并行持久化链路。 | Spring Kafka, MyBatis-Plus, MySQL, Sentinel |
| `ai-analysis-service` | 智能运维助手（`/api/ai/chat`）。LangChain4j + OpenAI，RAG 检索本地知识库 + Elasticsearch 日志，Redis 会话记忆 + 摘要，意图分类，工具调用（Kafka 指标查询、日志搜索），响应缓存。 | LangChain4j, Elasticsearch, Redis |
| `alert-service`、`query-service`、`stream-service`、`infra` | 骨架服务（仅有启动类，无业务逻辑） | Spring Boot starter |

### 统一事件模型

所有服务间通过 Kafka 传递此模型，禁止自定义不同结构：

```java
// common-lib: org.zc.common.Event
eventId, deviceId, timestamp, type, payload (Map<String, Object>)
```

## 开发规范

- **分层架构**：controller → service → repository，controller 禁止直接操作数据库，DTO 与 Entity 严格隔离。
- **统一返回**：所有接口返回 `Result<T>`（code=0 成功，code=1 失败）。
- **日志追踪**：关键路径日志必须通过 `MDC.putCloseable("traceId", ...)` 携带 traceId。
- **异常处理**：使用 `@ControllerAdvice` 全局异常处理，禁止 try-catch 吞异常。
- **幂等性**：所有写操作使用 `eventId` 去重。
- **配置管理**：配置必须从 `application.yml` 和 `@ConfigurationProperties` 读取，禁止硬编码。
- **Kafka 约束**：服务间事件传输必须通过 Kafka，禁止直接传递。Kafka 消费必须异步 + 线程池，禁止阻塞监听线程。
- **Lombok**：数据类统一使用 `@Data`。

## Kafka Topic

- `event-topic` — 原始接入事件（ingest 生产，consumer 消费）
- `event-storage-topic` — 待持久化事件（consumer 生产，storage 消费）
- 摄像头相关 Topic 在各环境 YAML 中配置

## 失败处理

失败事件封装为 `FailedEventMessage` / `CameraFailureMessage`（common-lib），在各阶段发布到失败 Topic。字段 `stage`、`reason`、`attempts`、`failedAt` 记录失败位置和原因。
