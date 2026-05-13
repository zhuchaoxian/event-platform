package org.zc.storage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zc.common.Result;
import org.zc.storage.entity.CameraRecord;
import org.zc.storage.mapper.CameraMapper;

@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class CameraQueryController {

    private final CameraMapper cameraMapper;

    @GetMapping("/cameras")
    public Result<Page<CameraRecord>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "cameraId", required = false) String cameraId,
            @RequestParam(name = "cameraName", required = false) String cameraName,
            @RequestParam(name = "status", required = false) Integer status) {

        LambdaQueryWrapper<CameraRecord> wrapper = new LambdaQueryWrapper<>();
        if (cameraId != null && !cameraId.isBlank()) {
            wrapper.like(CameraRecord::getCameraId, cameraId);
        }
        if (cameraName != null && !cameraName.isBlank()) {
            wrapper.like(CameraRecord::getCameraName, cameraName);
        }
        if (status != null) {
            wrapper.eq(CameraRecord::getStatus, status);
        }
        wrapper.orderByDesc(CameraRecord::getCreatedAt);

        Page<CameraRecord> result = cameraMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.ok(result);
    }
}
