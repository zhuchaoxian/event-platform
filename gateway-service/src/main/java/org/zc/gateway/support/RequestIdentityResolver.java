package org.zc.gateway.support;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequestIdentityResolver {

    public String resolveDeviceId(ServerHttpRequest request) {
        String deviceId = firstNonBlank(
            request.getHeaders().getFirst("X-Device-Id"),
            request.getHeaders().getFirst("deviceId"),
            request.getQueryParams().getFirst("deviceId")
        );
        return StringUtils.hasText(deviceId) ? deviceId.trim() : null;
    }

    public String resolveClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddress() == null ? null : request.getRemoteAddress().getAddress().getHostAddress();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
