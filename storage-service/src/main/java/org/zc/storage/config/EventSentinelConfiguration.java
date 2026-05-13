package org.zc.storage.config;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.zc.storage.sentinel.EventSentinelFacade;

@Configuration
public class EventSentinelConfiguration {

    private final EventProperties eventProperties;

    public EventSentinelConfiguration(EventProperties eventProperties) {
        this.eventProperties = eventProperties;
    }

    @PostConstruct
    public void loadRules() {
        EventProperties.Sentinel sentinel = eventProperties.getSentinel();
        mergeFlowRules(sentinel);
        mergeDegradeRules(sentinel);
    }

    private void mergeFlowRules(EventProperties.Sentinel sentinel) {
        List<FlowRule> rules = new ArrayList<>();
        for (FlowRule existingRule : FlowRuleManager.getRules()) {
            if (!EventSentinelFacade.PERSIST_RESOURCE.equals(existingRule.getResource())) {
                rules.add(existingRule);
            }
        }

        if (sentinel.isEnabled() && sentinel.getPersistQps() > 0) {
            FlowRule rule = new FlowRule();
            rule.setResource(EventSentinelFacade.PERSIST_RESOURCE);
            rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            rule.setCount(sentinel.getPersistQps());
            rules.add(rule);
        }

        FlowRuleManager.loadRules(rules);
    }

    private void mergeDegradeRules(EventProperties.Sentinel sentinel) {
        List<DegradeRule> rules = new ArrayList<>();
        for (DegradeRule existingRule : DegradeRuleManager.getRules()) {
            if (!EventSentinelFacade.PERSIST_RESOURCE.equals(existingRule.getResource())) {
                rules.add(existingRule);
            }
        }

        if (sentinel.isEnabled() && sentinel.getPersistExceptionCount() > 0) {
            DegradeRule rule = new DegradeRule();
            rule.setResource(EventSentinelFacade.PERSIST_RESOURCE);
            rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);
            rule.setCount(sentinel.getPersistExceptionCount());
            rule.setTimeWindow(Math.max(1, sentinel.getDegradeWindowSeconds()));
            rule.setMinRequestAmount(Math.max(1, sentinel.getMinRequestAmount()));
            rule.setStatIntervalMs(Math.max(1000, sentinel.getStatIntervalMs()));
            rules.add(rule);
        }

        DegradeRuleManager.loadRules(rules);
    }
}
