package org.zc.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zc.gateway.config.GatewayProperties;
import org.zc.gateway.exception.GatewayException;

@Service
@RequiredArgsConstructor
public class BlacklistService {
    private final GatewayProperties gatewayProperties;

    public void validate(String clientIp, String deviceId) {
        if (StringUtils.hasText(deviceId) && gatewayProperties.getBlacklist().getDeviceIds().contains(deviceId.trim())) {
            throw new GatewayException(HttpStatus.FORBIDDEN, 40301, "device is blocked");
        }
        if (StringUtils.hasText(clientIp) && gatewayProperties.getBlacklist().getIps().contains(clientIp.trim())) {
            throw new GatewayException(HttpStatus.FORBIDDEN, 40302, "ip is blocked");
        }
    }
}
