package org.zc.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zc.alert.entity.AlertRecord;
import org.zc.alert.mapper.AlertRecordMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AlertRecordService {

    private final AlertRecordMapper alertRecordMapper;

    public Page<AlertRecord> list(int page, int size, String status, String eventType, String deviceId) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<AlertRecord>()
                .eq(status != null && !status.isBlank(), AlertRecord::getStatus, status)
                .eq(eventType != null && !eventType.isBlank(), AlertRecord::getEventType, eventType)
                .eq(deviceId != null && !deviceId.isBlank(), AlertRecord::getDeviceId, deviceId)
                .orderByDesc(AlertRecord::getTriggeredAt);
        return alertRecordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public AlertRecord getById(Long id) {
        return alertRecordMapper.selectById(id);
    }

    public void ack(Long id) {
        AlertRecord record = alertRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("alert record not found: " + id);
        }
        record.setStatus("ACKED");
        record.setAckedAt(LocalDateTime.now());
        alertRecordMapper.updateById(record);
    }

    public void resolve(Long id) {
        AlertRecord record = alertRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("alert record not found: " + id);
        }
        record.setStatus("RESOLVED");
        record.setResolvedAt(LocalDateTime.now());
        alertRecordMapper.updateById(record);
    }

    public void save(AlertRecord record) {
        alertRecordMapper.insert(record);
    }
}
