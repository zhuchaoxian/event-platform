package org.zc.storage.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.zc.storage.sentinel.EventSentinelFacade;

class StorageEventSentinelConfigurationTest {

    @AfterEach
    void cleanUpRules() {
        FlowRuleManager.loadRules(Collections.emptyList());
        DegradeRuleManager.loadRules(Collections.emptyList());
    }

    @Test
    void shouldLoadFlowAndDegradeRulesForEventStoragePersistence() {
        EventProperties properties = new EventProperties();
        properties.getSentinel().setEnabled(true);
        properties.getSentinel().setPersistQps(88d);
        properties.getSentinel().setPersistExceptionCount(6d);
        properties.getSentinel().setDegradeWindowSeconds(12);
        properties.getSentinel().setMinRequestAmount(8);
        properties.getSentinel().setStatIntervalMs(15000);

        new EventSentinelConfiguration(properties).loadRules();

        FlowRule flowRule = FlowRuleManager.getRules().stream()
                .filter(rule -> EventSentinelFacade.PERSIST_RESOURCE.equals(rule.getResource()))
                .findFirst()
                .orElseThrow();
        assertEquals(88d, flowRule.getCount());

        DegradeRule degradeRule = DegradeRuleManager.getRules().stream()
                .filter(rule -> EventSentinelFacade.PERSIST_RESOURCE.equals(rule.getResource()))
                .findFirst()
                .orElseThrow();
        assertEquals(6d, degradeRule.getCount());
        assertEquals(12, degradeRule.getTimeWindow());
        assertEquals(8, degradeRule.getMinRequestAmount());
        assertEquals(15000, degradeRule.getStatIntervalMs());
    }

    @Test
    void shouldRemoveEventStorageRulesWhenSentinelIsDisabled() {
        EventProperties properties = new EventProperties();
        properties.getSentinel().setEnabled(false);

        FlowRule existingFlowRule = new FlowRule();
        existingFlowRule.setResource("camera-storage-persist");
        existingFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        existingFlowRule.setCount(1d);
        FlowRuleManager.loadRules(Collections.singletonList(existingFlowRule));

        DegradeRule existingDegradeRule = new DegradeRule();
        existingDegradeRule.setResource("camera-storage-persist");
        existingDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);
        existingDegradeRule.setCount(1d);
        existingDegradeRule.setTimeWindow(1);
        DegradeRuleManager.loadRules(Collections.singletonList(existingDegradeRule));

        new EventSentinelConfiguration(properties).loadRules();

        assertTrue(FlowRuleManager.getRules().stream()
                .noneMatch(rule -> EventSentinelFacade.PERSIST_RESOURCE.equals(rule.getResource())));
        assertTrue(DegradeRuleManager.getRules().stream()
                .noneMatch(rule -> EventSentinelFacade.PERSIST_RESOURCE.equals(rule.getResource())));
        assertEquals(1, FlowRuleManager.getRules().size());
        assertEquals(1, DegradeRuleManager.getRules().size());
    }
}
