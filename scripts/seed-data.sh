#!/bin/bash
# ============================================================
# 种入10+条模拟事件数据，触发告警记录
# 前置条件: MySQL(3306)、Kafka(9092)、Redis(6379) 已启动
#          gateway-service、ingest-service、consumer-service、
#          storage-service、alert-service 已启动
# ============================================================

MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:-root}"
MYSQL_DB="event_platform"

GATEWAY="${GATEWAY:-http://localhost:8080}"
NOW=$(date +%s%3N)   # 毫秒时间戳

# ---------- Step 1: 插入告警规则 ----------
echo "=== 插入 4 条告警规则 ==="

mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASS" --default-character-set=utf8mb4 "$MYSQL_DB" <<'SQL'
INSERT INTO alert_rule (rule_name, event_type, rule_type, threshold, cooldown_s, device_id, enabled)
VALUES
('违停时长告警',   'PARKING',    'THRESHOLD', '{"field":"duration","operator":"gt","value":300}',      300, NULL, 1),
('严重拥堵告警',   'CONGESTION', 'THRESHOLD', '{"field":"vehicleCount","operator":"gt","value":50}',    300, NULL, 1),
('逆行频发告警',   'WRONG_WAY',  'FREQUENCY', '{"windowSeconds":60,"maxCount":2}',                     300, NULL, 1),
('持续拥堵告警',   'CONGESTION', 'DURATION',  '{"maxDurationSeconds":60}',                             300, NULL, 1)
ON DUPLICATE KEY UPDATE rule_name=VALUES(rule_name);
SQL

echo "规则插入完成"
echo ""

# ---------- Step 2: 发送模拟事件 ----------
echo "=== 发送 11 条模拟事件 ==="

send_event() {
  local device_id="$1" event_type="$2" payload="$3" desc="$4"
  local event_id="seed-$(printf '%04d' $((i=i+1)))"
  echo "[$i] $desc"
  curl -s -X POST "$GATEWAY/api/ingest/event/report" \
    -H "Content-Type: application/json; charset=utf-8" \
    -d "{\"eventId\":\"$event_id\",\"deviceId\":\"$device_id\",\"timestamp\":$((NOW + i * 1000)),\"type\":\"$event_type\",\"payload\":$payload}" \
    && echo ""
}

i=0

# --- 违停时长 (阈值: 时长 > 300秒) ---
send_event "camera-01" "PARKING"    '{"duration":450,  "plate":"A12345"}' "camera-01 违停450秒 → 预期触发告警(>300)"
send_event "camera-01" "PARKING"    '{"duration":200,  "plate":"B67890"}' "camera-01 违停200秒 → 不触发(低于阈值)"
send_event "camera-02" "PARKING"    '{"duration":600,  "plate":"C11111"}' "camera-02 违停600秒 → 预期触发告警(不同设备)"

# --- 严重拥堵 (阈值: 车辆数 > 50) ---
send_event "camera-03" "CONGESTION" '{"vehicleCount":65, "level":"high"}'  "camera-03 车数65 → 预期触发告警(>50)"
send_event "camera-03" "CONGESTION" '{"vehicleCount":30, "level":"low"}'   "camera-03 车数30 → 不触发(低于阈值)"

# --- 逆行频发 (频率: 60秒内 > 2次) ---
send_event "camera-01" "WRONG_WAY"  '{"plate":"D22222", "lane":2}'         "camera-01 逆行#1 → count=1"
send_event "camera-01" "WRONG_WAY"  '{"plate":"E33333", "lane":2}'         "camera-01 逆行#2 → count=2"
send_event "camera-01" "WRONG_WAY"  '{"plate":"F44444", "lane":2}'         "camera-01 逆行#3 → 预期触发告警(>2)"

# --- 持续拥堵 (持续: 首次后60秒内再触发) ---
send_event "camera-04" "CONGESTION" '{"vehicleCount":55, "level":"mid"}'   "camera-04 首次拥堵 → 不触发(设置持续标记)"
send_event "camera-04" "CONGESTION" '{"vehicleCount":58, "level":"high"}'  "camera-04 再次拥堵 → 预期触发告警(持续中)"

# --- 逆行事件补充 (不同设备，用于丰富数据) ---
send_event "camera-05" "WRONG_WAY"  '{"plate":"G55555", "lane":1}'         "camera-05 逆行事件 → 不触发(仅1次，未超频率)"

echo ""
echo "=== 完成 ==="
echo "预期告警记录数: 5 条"
echo "查询告警: curl -s http://localhost:8080/api/alert/records -H 'Authorization: Bearer <token>'"
