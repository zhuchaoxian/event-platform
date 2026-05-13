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
import org.zc.storage.sentinel.CameraSentinelFacade;

class CameraStorageSentinelConfigurationTest {

    @AfterEach
    void cleanUpRules() {
        FlowRuleManager.loadRules(Collections.emptyList());
        DegradeRuleManager.loadRules(Collections.emptyList());
    }

    @Test
    void shouldLoadFlowAndDegradeRulesForCameraResources() {
        CameraProperties properties = new CameraProperties();
        properties.getSentinel().setEnabled(true);
        properties.getSentinel().setEnqueueQps(123d);
        properties.getSentinel().setPersistQps(45d);
        properties.getSentinel().setPersistExceptionCount(6d);
        properties.getSentinel().setDegradeWindowSeconds(12);
        properties.getSentinel().setMinRequestAmount(8);
        properties.getSentinel().setStatIntervalMs(15000);

        new CameraSentinelConfiguration(properties).loadRules();

        FlowRule enqueueRule = FlowRuleManager.getRules().stream()
                .filter(rule -> CameraSentinelFacade.ENQUEUE_RESOURCE.equals(rule.getResource()))
                .findFirst()
                .orElseThrow();
        assertEquals(123d, enqueueRule.getCount());

        FlowRule persistRule = FlowRuleManager.getRules().stream()
                .filter(rule -> CameraSentinelFacade.PERSIST_RESOURCE.equals(rule.getResource()))
                .findFirst()
                .orElseThrow();
        assertEquals(45d, persistRule.getCount());

        DegradeRule degradeRule = DegradeRuleManager.getRules().stream()
                .filter(rule -> CameraSentinelFacade.PERSIST_RESOURCE.equals(rule.getResource()))
                .findFirst()
                .orElseThrow();
        assertEquals(6d, degradeRule.getCount());
        assertEquals(12, degradeRule.getTimeWindow());
        assertEquals(8, degradeRule.getMinRequestAmount());
        assertEquals(15000, degradeRule.getStatIntervalMs());
    }

    @Test
    void shouldRemoveCameraRulesWhenSentinelIsDisabled() {
        CameraProperties properties = new CameraProperties();
        properties.getSentinel().setEnabled(false);

        FlowRule eventRule = new FlowRule();
        eventRule.setResource("event-storage-persist");
        eventRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        eventRule.setCount(1d);
        FlowRuleManager.loadRules(Collections.singletonList(eventRule));

        DegradeRule eventDegradeRule = new DegradeRule();
        eventDegradeRule.setResource("event-storage-persist");
        eventDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);
        eventDegradeRule.setCount(1d);
        eventDegradeRule.setTimeWindow(1);
        DegradeRuleManager.loadRules(Collections.singletonList(eventDegradeRule));

        new CameraSentinelConfiguration(properties).loadRules();

        assertTrue(FlowRuleManager.getRules().stream()
                .noneMatch(rule -> CameraSentinelFacade.ENQUEUE_RESOURCE.equals(rule.getResource())
                        || CameraSentinelFacade.PERSIST_RESOURCE.equals(rule.getResource())));
        assertTrue(DegradeRuleManager.getRules().stream()
                .noneMatch(rule -> CameraSentinelFacade.PERSIST_RESOURCE.equals(rule.getResource())));
        assertEquals(1, FlowRuleManager.getRules().size());
        assertEquals(1, DegradeRuleManager.getRules().size());
    }
}
