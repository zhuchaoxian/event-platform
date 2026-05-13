package org.zc.alert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zc.alert.engine.AlertRuleEngine;
import org.zc.alert.entity.AlertRecord;
import org.zc.alert.producer.AlertNotificationProducer;
import org.zc.common.Event;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEventService {

    private final AlertRuleEngine alertRuleEngine;
    private final AlertRecordService alertRecordService;
    private final AlertNotificationProducer alertNotificationProducer;

    public void process(Event event) {
        AlertRecord record = alertRuleEngine.evaluate(event);
        if (record == null) {
            return;
        }
        alertRecordService.save(record);
        log.info("Alert triggered. alertId={}, ruleId={}, deviceId={}, eventType={}",
                record.getId(), record.getRuleId(), record.getDeviceId(), record.getEventType());
        alertNotificationProducer.send(record);
    }
}
