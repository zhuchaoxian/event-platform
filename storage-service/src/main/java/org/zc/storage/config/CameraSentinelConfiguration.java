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
import org.zc.storage.sentinel.CameraSentinelFacade;

@Configuration
public class CameraSentinelConfiguration {

    private final CameraProperties cameraProperties;

    public CameraSentinelConfiguration(CameraProperties cameraProperties) {
        this.cameraProperties = cameraProperties;
    }

    @PostConstruct
    public void loadRules() {
        CameraProperties.Sentinel sentinel = cameraProperties.getSentinel();
        mergeFlowRules(sentinel);
        mergeDegradeRules(sentinel);
    }

    private void mergeFlowRules(CameraProperties.Sentinel sentinel) {
        List<FlowRule> rules = new ArrayList<>();
        for (FlowRule existingRule : FlowRuleManager.getRules()) {
            String resource = existingRule.getResource();
            if (!CameraSentinelFacade.ENQUEUE_RESOURCE.equals(resource)
                    && !CameraSentinelFacade.PERSIST_RESOURCE.equals(resource)) {
                rules.add(existingRule);
            }
        }

        if (sentinel.isEnabled() && sentinel.getEnqueueQps() > 0) {
            FlowRule enqueueRule = new FlowRule();
            enqueueRule.setResource(CameraSentinelFacade.ENQUEUE_RESOURCE);
            enqueueRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            enqueueRule.setCount(sentinel.getEnqueueQps());
            rules.add(enqueueRule);
        }

        if (sentinel.isEnabled() && sentinel.getPersistQps() > 0) {
            FlowRule persistRule = new FlowRule();
            persistRule.setResource(CameraSentinelFacade.PERSIST_RESOURCE);
            persistRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            persistRule.setCount(sentinel.getPersistQps());
            rules.add(persistRule);
        }

        FlowRuleManager.loadRules(rules);
    }

    private void mergeDegradeRules(CameraProperties.Sentinel sentinel) {
        List<DegradeRule> rules = new ArrayList<>();
        for (DegradeRule existingRule : DegradeRuleManager.getRules()) {
            if (!CameraSentinelFacade.PERSIST_RESOURCE.equals(existingRule.getResource())) {
                rules.add(existingRule);
            }
        }

        if (sentinel.isEnabled() && sentinel.getPersistExceptionCount() > 0) {
            DegradeRule persistRule = new DegradeRule();
            persistRule.setResource(CameraSentinelFacade.PERSIST_RESOURCE);
            persistRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);
            persistRule.setCount(sentinel.getPersistExceptionCount());
            persistRule.setTimeWindow(Math.max(1, sentinel.getDegradeWindowSeconds()));
            persistRule.setMinRequestAmount(Math.max(1, sentinel.getMinRequestAmount()));
            persistRule.setStatIntervalMs(Math.max(1000, sentinel.getStatIntervalMs()));
            rules.add(persistRule);
        }

        DegradeRuleManager.loadRules(rules);
    }
}
