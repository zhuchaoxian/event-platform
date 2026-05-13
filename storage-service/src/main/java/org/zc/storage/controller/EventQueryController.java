package org.zc.storage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zc.common.Result;
import org.zc.storage.entity.EventRecord;
import org.zc.storage.mapper.EventMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class EventQueryController {

    private final EventMapper eventMapper;

    @GetMapping("/events")
    public Result<Page<EventRecord>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "deviceId", required = false) String deviceId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "startTime", required = false) Long startTime,
            @RequestParam(name = "endTime", required = false) Long endTime) {

        LambdaQueryWrapper<EventRecord> wrapper = new LambdaQueryWrapper<>();
        if (deviceId != null && !deviceId.isBlank()) {
            wrapper.eq(EventRecord::getDeviceId, deviceId);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(EventRecord::getEventType, type);
        }
        if (startTime != null) {
            wrapper.ge(EventRecord::getEventTimestamp, startTime);
        }
        if (endTime != null) {
            wrapper.le(EventRecord::getEventTimestamp, endTime);
        }
        wrapper.orderByDesc(EventRecord::getCreatedAt);

        Page<EventRecord> result = eventMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.ok(result);
    }

    @GetMapping("/events/stats")
    public Result<Map<String, Object>> stats(
            @RequestParam(name = "deviceId", required = false) String deviceId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "startTime", required = false) Long startTime,
            @RequestParam(name = "endTime", required = false) Long endTime) {

        LambdaQueryWrapper<EventRecord> wrapper = new LambdaQueryWrapper<>();
        if (deviceId != null && !deviceId.isBlank()) {
            wrapper.eq(EventRecord::getDeviceId, deviceId);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(EventRecord::getEventType, type);
        }
        if (startTime != null) {
            wrapper.ge(EventRecord::getEventTimestamp, startTime);
        }
        if (endTime != null) {
            wrapper.le(EventRecord::getEventTimestamp, endTime);
        }

        List<EventRecord> records = eventMapper.selectList(wrapper);
        Map<String, Long> typeCounts = new HashMap<>();
        for (EventRecord r : records) {
            String eventType = r.getEventType();
            typeCounts.merge(eventType != null ? eventType : "unknown", 1L, Long::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", records.size());
        result.put("typeCounts", typeCounts);
        return Result.ok(result);
    }
}
