-- event_platform.db_camera definition

CREATE TABLE `db_camera` (
                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
                             `camera_id` varchar(64) NOT NULL,
                             `camera_name` varchar(255) DEFAULT NULL,
                             `latitude` double DEFAULT NULL,
                             `longitude` double DEFAULT NULL,
                             `status` int(11) DEFAULT NULL,
                             `create_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uk_camera_info_camera_id` (`camera_id`),
                             KEY `idx_camera_info_ts` (`create_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- event_platform.alert_rule definition

CREATE TABLE alert_rule (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name   VARCHAR(128)  NOT NULL COMMENT '规则名称',
    event_type  VARCHAR(64)   NOT NULL COMMENT '事件类型',
    rule_type   VARCHAR(32)   NOT NULL COMMENT '规则类型: THRESHOLD/FREQUENCY/DURATION',
    threshold   JSON          NOT NULL COMMENT '规则阈值配置',
    cooldown_s  INT           NOT NULL DEFAULT 300 COMMENT '冷却时间(秒)',
    device_id   VARCHAR(64)   DEFAULT NULL COMMENT '设备ID, NULL=全局',
    enabled     TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '启用',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- event_platform.alert_record definition

CREATE TABLE alert_record (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id      BIGINT        DEFAULT NULL COMMENT '规则ID',
    rule_name    VARCHAR(128)  DEFAULT NULL COMMENT '规则名称快照',
    event_id     VARCHAR(64)   NOT NULL COMMENT '事件ID',
    device_id    VARCHAR(64)   NOT NULL COMMENT '设备ID',
    event_type   VARCHAR(64)   NOT NULL COMMENT '事件类型',
    event_data   JSON          DEFAULT NULL COMMENT '事件payload',
    alert_level  VARCHAR(16)   NOT NULL DEFAULT 'WARNING' COMMENT '告警级别',
    message      VARCHAR(512)  NOT NULL COMMENT '告警描述',
    status       VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    triggered_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acked_at     TIMESTAMP     DEFAULT NULL,
    resolved_at  TIMESTAMP     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;