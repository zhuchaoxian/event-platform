# Kafka Runbook

## 1. 服务说明

Kafka 用于 IoT 设备事件、告警消息、审计日志与 AI Ops 事件流转。

当前集群：

* Broker: 3节点
* Topic 分区数: 64
* 副本因子: 3
* 消息保留: 7天
* 部署模式: KRaft

---

# 2. 常见告警

| 告警                          | 含义        |
| --------------------------- | --------- |
| Consumer Lag High           | 消费堆积      |
| Under Replicated Partitions | 副本同步异常    |
| Broker Down                 | Broker 宕机 |
| ISR Shrink                  | ISR 缩小    |
| Produce Timeout             | 生产超时      |

---

# 3. Consumer Lag 持续增长排查

## 3.1 查看消费堆积

```bash
kafka-consumer-groups.sh \
--bootstrap-server localhost:9092 \
--describe \
--group iot-group
```

重点关注：

* CURRENT-OFFSET
* LOG-END-OFFSET
* LAG

---

## 3.2 判断是否消费能力不足

检查：

* 消费线程数
* Topic partition 数
* CPU 使用率
* GC 情况

原则：

```text
consumer线程数 <= partition数
```

---

## 3.3 查看是否发生 Rebalance

日志关键字：

```text
Rebalance
Member removed
Heartbeat timeout
```

常见原因：

* 消费处理过慢
* poll 间隔过长
* GC STW
* 网络抖动

---

## 3.4 排查消息处理慢

检查：

* 数据库慢 SQL
* Redis 超时
* 第三方接口阻塞
* 批量提交过大

建议：

* 批量消费
* 异步处理
* 限流降级

---

# 4. Broker 宕机处理

## 4.1 查看 Broker 状态

```bash
jps
```

或：

```bash
systemctl status kafka
```

---

## 4.2 查看日志

```bash
tail -200f server.log
```

重点关键字：

```text
OutOfMemoryError
Disk error
Leader not available
```

---

## 4.3 检查磁盘

```bash
df -h
```

磁盘超过 85% 需要清理。

---

## 4.4 重启 Broker

```bash
systemctl restart kafka
```

重启后确认：

* ISR 恢复
* Partition 正常
* Consumer 恢复

---

# 5. ISR Shrink 处理

ISR 缩小表示副本同步异常。

检查：

* 网络延迟
* Broker CPU
* Broker GC
* 磁盘 IO

查看：

```bash
kafka-topics.sh \
--describe \
--bootstrap-server localhost:9092
```

重点：

```text
ISR
Leader
Replicas
```

---

# 6. 消息积压应急方案

## 方案1：扩容 Consumer

增加：

```text
consumer实例数
```

注意：

```text
实例数不要超过 partition 数
```

---

## 方案2：临时跳过历史消息

仅限非核心业务。

```java
auto.offset.reset=latest
```

---

## 方案3：增加 Partition

```bash
kafka-topics.sh \
--alter \
--topic iot-event-topic \
--partitions 128 \
--bootstrap-server localhost:9092
```

注意：

* 只能增加
* 无法减少

---

# 7. Topic 规范

Topic 命名：

```text
iot-device-event
iot-device-alert
iot-audit-log
ai-ops-event
```

禁止：

```text
test
demo
tmp
```

---

# 8. Producer 最佳实践

推荐配置：

```properties
acks=all
retries=3
enable.idempotence=true
compression.type=lz4
batch.size=65536
linger.ms=5
```

---

# 9. Consumer 最佳实践

推荐：

```properties
enable-auto-commit=false
max.poll.records=500
fetch.min.bytes=1
```

消费逻辑：

```text
拉取
→ 批量处理
→ 手动提交 offset
```

---

# 10. 高并发 IoT 场景建议

IoT 平台建议：

* Topic 按事件类型拆分
* deviceId hash 分片
* Kafka + Redis 解耦
* 削峰填谷
* 批量写入 DB

推荐链路：

```text
MQTT
→ EMQX
→ Kafka
→ Consumer
→ Redis/ES/MySQL
```

---

# 11. 监控指标

重点监控：

| 指标                          | 含义       |
| --------------------------- | -------- |
| Messages In/sec             | 写入TPS    |
| Bytes In/sec                | 流量       |
| Consumer Lag                | 消费堆积     |
| Request Handler Avg Idle    | Broker负载 |
| Under Replicated Partitions | 副本异常     |

---

# 12. 故障恢复 Checklist

发生故障后：

* [ ] Broker 是否存活
* [ ] Topic 是否存在
* [ ] ISR 是否恢复
* [ ] Lag 是否恢复
* [ ] Consumer 是否正常
* [ ] 是否发生 Rebalance
* [ ] 是否存在磁盘满
* [ ] 是否存在 GC Full GC

---

# 13. 联系人

| 模块          | 负责人   |
| ----------- | ----- |
| Kafka 集群    | 平台组   |
| Consumer 服务 | 中台组   |
| AI Ops      | 运维AI组 |
