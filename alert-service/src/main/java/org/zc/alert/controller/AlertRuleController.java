package org.zc.alert.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zc.alert.dto.AlertRuleCreateRequest;
import org.zc.alert.dto.AlertRuleUpdateRequest;
import org.zc.alert.entity.AlertRule;
import org.zc.alert.service.AlertRuleService;
import org.zc.common.Result;

@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @PostMapping
    public Result<AlertRule> create(@Valid @RequestBody AlertRuleCreateRequest request) {
        AlertRule rule = new AlertRule();
        rule.setRuleName(request.getRuleName());
        rule.setEventType(request.getEventType());
        rule.setRuleType(request.getRuleType());
        rule.setThreshold(request.getThreshold());
        rule.setCooldownS(request.getCooldownS());
        rule.setDeviceId(request.getDeviceId());
        return Result.ok(alertRuleService.create(rule));
    }

    @GetMapping
    public Result<Page<AlertRule>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "eventType", required = false) String eventType) {
        return Result.ok(alertRuleService.list(page, size, eventType));
    }

    @GetMapping("/{id}")
    public Result<AlertRule> getById(@PathVariable Long id) {
        return Result.ok(alertRuleService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<AlertRule> update(@PathVariable Long id, @RequestBody AlertRuleUpdateRequest request) {
        AlertRule rule = new AlertRule();
        rule.setRuleName(request.getRuleName());
        rule.setEventType(request.getEventType());
        rule.setRuleType(request.getRuleType());
        rule.setThreshold(request.getThreshold());
        rule.setCooldownS(request.getCooldownS());
        rule.setDeviceId(request.getDeviceId());
        return Result.ok(alertRuleService.update(id, rule));
    }

    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        alertRuleService.disable(id);
        return Result.ok(null);
    }
}
