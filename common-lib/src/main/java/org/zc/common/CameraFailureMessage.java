package org.zc.common;

import lombok.Data;

@Data
public class CameraFailureMessage {
    private CameraMessage camera;
    private String stage;
    private String reason;
    private int attempts;
    private long failedAt;
}
