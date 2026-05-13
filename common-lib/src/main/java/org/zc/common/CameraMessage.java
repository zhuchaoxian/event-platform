package org.zc.common;

import lombok.Data;

@Data
public class CameraMessage {
    private String cameraId;
    private String cameraName;
    private Double latitude;
    private Double longitude;
    private Integer status;
}
