# Event Platform · 高并发事件中台

基于 **Spring Boot 3.2.5 + Spring Cloud 2023.0.1 + Kafka** 的事件驱动微服务架构，目标吞吐 **8000+ QPS**。

## 架构

```
HTTP/MQTT → ingest-service → Kafka → gateway-service（API 网关）
                                       → consumer-service → Kafka → storage-service → MySQL
                                       → alert-service
                                       → query-service
                                       → stream-service
                                       → ai-analysis-service（RAG 智能运维助手）
```

## 模块

| 模块 | 职责 | 技术栈 |
|---|---|---|
| `common-lib` | 共享领域模型：Event、Result、CameraMessage 等 | Lombok |
| `ingest-service` | 双通道接入：HTTP POST `/event/report` + MQTT，事件标准化后投递 Kafka | Spring Kafka, MQTT |
| `gateway-service` | API 网关，JWT 认证，IP/设备黑名单，Redis 限流 | Spring Cloud Gateway, Redis, jjwt |
| `consumer-service` | Kafka 消费，线程池异步处理，投递存储 Topic | Spring Kafka |
| `storage-service` | 批量消费，MyBatis-Plus 写入 MySQL，Sentinel 熔断重试 | Spring Kafka, MyBatis-Plus, Sentinel |
| `ai-analysis-service` | 智能运维助手 `/api/ai/chat`，LangChain4j + RAG + ES 日志 | LangChain4j, Elasticsearch, Redis |
| `alert-service` | 告警规则引擎与通知 | Spring Boot |
| `query-service` | 事件查询服务 | Spring Boot |
| `stream-service` | 流式处理服务 | Spring Boot |
