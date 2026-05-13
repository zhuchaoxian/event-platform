package org.zc.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zc.alert.entity.AlertRule;
import org.zc.alert.mapper.AlertRuleMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleMapper alertRuleMapper;

    public AlertRule create(AlertRule rule) {
        rule.setEnabled(1);
        alertRuleMapper.insert(rule);
        return rule;
    }

    public Page<AlertRule> list(int page, int size, String eventType) {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<AlertRule>()
                .eq(eventType != null && !eventType.isBlank(), AlertRule::getEventType, eventType)
                .orderByDesc(AlertRule::getCreatedAt);
        return alertRuleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public AlertRule getById(Long id) {
        return alertRuleMapper.selectById(id);
    }

    public AlertRule update(Long id, AlertRule rule) {
        AlertRule existing = alertRuleMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("rule not found: " + id);
        }
        rule.setId(id);
        alertRuleMapper.updateById(rule);
        return alertRuleMapper.selectById(id);
    }

    public void disable(Long id) {
        AlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("rule not found: " + id);
        }
        rule.setEnabled(0);
        alertRuleMapper.updateById(rule);
    }
}
