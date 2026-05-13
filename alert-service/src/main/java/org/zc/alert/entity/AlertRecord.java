package org.zc.alert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_record")
public class AlertRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private String ruleName;
    private String eventId;
    private String deviceId;
    private String eventType;
    private String eventData;
    private String alertLevel;
    private String message;
    private String status;
    private LocalDateTime triggeredAt;
    private LocalDateTime ackedAt;
    private LocalDateTime resolvedAt;
}
