package org.zc.alert.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.zc.alert.config.AlertProperties;
import org.zc.alert.entity.AlertRecord;
import org.zc.alert.entity.AlertRule;
import org.zc.alert.mapper.AlertRecordMapper;
import org.zc.alert.mapper.AlertRuleMapper;
import org.zc.common.Event;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRuleEngine {

    private static final String COOLDOWN_KEY = "alert:cooldown:%d:%s";
    private static final String FREQ_KEY = "alert:freq:%d:%s";
    private static final String DURATION_KEY = "alert:duration:%d:%s";

    private final AlertRuleMapper alertRuleMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final StringRedisTemplate redisTemplate;
    private final AlertProperties alertProperties;
    private final ObjectMapper objectMapper;

    public AlertRecord evaluate(Event event) {
        List<AlertRule> rules = findMatchingRules(event);
        for (AlertRule rule : rules) {
            if (!isCooldownPassed(rule, event.getDeviceId())) {
                continue;
            }
            if (hasActiveAlert(rule, event.getDeviceId())) {
                continue;
            }
            boolean triggered = switch (rule.getRuleType()) {
                case "THRESHOLD" -> evaluateThreshold(rule, event);
                case "FREQUENCY" -> evaluateFrequency(rule, event);
                case "DURATION" -> evaluateDuration(rule, event);
                default -> false;
            };
            if (triggered) {
                markCooldown(rule, event.getDeviceId());
                return buildAlertRecord(rule, event);
            }
        }
        return null;
    }

    private List<AlertRule> findMatchingRules(Event event) {
        return alertRuleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>()
                        .eq(AlertRule::getEnabled, 1)
                        .eq(AlertRule::getEventType, event.getType())
                        .and(w -> w.isNull(AlertRule::getDeviceId)
                                .or().eq(AlertRule::getDeviceId, event.getDeviceId())));
    }

    private boolean evaluateThreshold(AlertRule rule, Event event) {
        try {
            Map<String, Object> config = objectMapper.readValue(
                    rule.getThreshold(), new TypeReference<Map<String, Object>>() {});
            String field = (String) config.get("field");
            String operator = (String) config.get("operator");
            Object expected = config.get("value");
            if (field == null || operator == null || expected == null || event.getPayload() == null) {
                return false;
            }
            Object actual = event.getPayload().get(field);
            if (actual == null) {
                return false;
            }
            return compare(actual, operator, expected);
        } catch (Exception e) {
            log.warn("Failed to evaluate threshold rule. ruleId={}", rule.getId(), e);
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean compare(Object actual, String operator, Object expected) {
        if (actual instanceof Number an && expected instanceof Number en) {
            double a = an.doubleValue();
            double e = en.doubleValue();
            return switch (operator) {
                case "gt" -> a > e;
                case "gte" -> a >= e;
                case "lt" -> a < e;
                case "lte" -> a <= e;
                case "eq" -> a == e;
                default -> false;
            };
        }
        if (actual instanceof Comparable ac && expected instanceof Comparable ec) {
            int cmp = ac.compareTo(ec);
            return switch (operator) {
                case "gt" -> cmp > 0;
                case "gte" -> cmp >= 0;
                case "lt" -> cmp < 0;
                case "lte" -> cmp <= 0;
                case "eq" -> cmp == 0;
                default -> false;
            };
        }
        return false;
    }

    private boolean evaluateFrequency(AlertRule rule, Event event) {
        try {
            Map<String, Object> config = objectMapper.readValue(
                    rule.getThreshold(), new TypeReference<Map<String, Object>>() {});
            int windowSeconds = config.containsKey("windowSeconds")
                    ? ((Number) config.get("windowSeconds")).intValue()
                    : alertProperties.getFrequency().getWindowSeconds();
            int maxCount = config.containsKey("maxCount")
                    ? ((Number) config.get("maxCount")).intValue()
                    : alertProperties.getFrequency().getDefaultMaxCount();

            String key = String.format(FREQ_KEY, rule.getId(), event.getDeviceId());
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return false;
            }
            if (count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return count > maxCount;
        } catch (Exception e) {
            log.warn("Failed to evaluate frequency rule. ruleId={}", rule.getId(), e);
            return false;
        }
    }

    private boolean evaluateDuration(AlertRule rule, Event event) {
        try {
            Map<String, Object> config = objectMapper.readValue(
                    rule.getThreshold(), new TypeReference<Map<String, Object>>() {});
            int maxDurationSeconds = config.containsKey("maxDurationSeconds")
                    ? ((Number) config.get("maxDurationSeconds")).intValue()
                    : 300;

            String key = String.format(DURATION_KEY, rule.getId(), event.getDeviceId());
            Boolean exists = redisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(exists)) {
                return true;
            }
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()),
                    Duration.ofSeconds(maxDurationSeconds));
            return false;
        } catch (Exception e) {
            log.warn("Failed to evaluate duration rule. ruleId={}", rule.getId(), e);
            return false;
        }
    }

    private boolean isCooldownPassed(AlertRule rule, String deviceId) {
        int cooldownSeconds = rule.getCooldownS() != null
                ? rule.getCooldownS()
                : alertProperties.getCooldown().getDefaultSeconds();
        String key = String.format(COOLDOWN_KEY, rule.getId(), deviceId);
        return Boolean.FALSE.equals(redisTemplate.hasKey(key));
    }

    private void markCooldown(AlertRule rule, String deviceId) {
        int cooldownSeconds = rule.getCooldownS() != null
                ? rule.getCooldownS()
                : alertProperties.getCooldown().getDefaultSeconds();
        String key = String.format(COOLDOWN_KEY, rule.getId(), deviceId);
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(cooldownSeconds));
    }

    private boolean hasActiveAlert(AlertRule rule, String deviceId) {
        return alertRecordMapper.exists(new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getRuleId, rule.getId())
                .eq(AlertRecord::getDeviceId, deviceId)
                .eq(AlertRecord::getStatus, "ACTIVE"));
    }

    private AlertRecord buildAlertRecord(AlertRule rule, Event event) {
        AlertRecord record = new AlertRecord();
        record.setRuleId(rule.getId());
        record.setRuleName(rule.getRuleName());
        record.setEventId(event.getEventId());
        record.setDeviceId(event.getDeviceId());
        record.setEventType(event.getType());
        record.setAlertLevel("WARNING");
        record.setMessage(String.format("%s 触发告警: %s", event.getDeviceId(), rule.getRuleName()));
        record.setStatus("ACTIVE");
        try {
            record.setEventData(objectMapper.writeValueAsString(event.getPayload()));
        } catch (Exception ignored) {
            record.setEventData("{}");
        }
        return record;
    }
}
