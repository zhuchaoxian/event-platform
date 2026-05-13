package org.zc.alert.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zc.alert.entity.AlertRecord;
import org.zc.alert.service.AlertRecordService;
import org.zc.common.Result;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class AlertRecordController {

    private final AlertRecordService alertRecordService;

    @GetMapping
    public Result<Page<AlertRecord>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "deviceId", required = false) String deviceId) {
        return Result.ok(alertRecordService.list(page, size, status, eventType, deviceId));
    }

    @GetMapping("/{id}")
    public Result<AlertRecord> getById(@PathVariable Long id) {
        return Result.ok(alertRecordService.getById(id));
    }

    @PostMapping("/{id}/ack")
    public Result<Void> ack(@PathVariable Long id) {
        alertRecordService.ack(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id) {
        alertRecordService.resolve(id);
        return Result.ok(null);
    }
}
