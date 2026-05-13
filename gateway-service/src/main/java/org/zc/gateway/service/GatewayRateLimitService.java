package org.zc.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zc.gateway.config.GatewayProperties;
import org.zc.gateway.exception.GatewayException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class GatewayRateLimitService {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayProperties gatewayProperties;

    public Mono<Void> validate(String clientIp, String deviceId) {
        return validateRule("ip", clientIp, gatewayProperties.getRateLimit().getIp())
            .then(validateRule("device", deviceId, gatewayProperties.getRateLimit().getDevice()));
    }

    private Mono<Void> validateRule(String dimension, String identifier, GatewayProperties.Rule rule) {
        if (!StringUtils.hasText(identifier) || rule == null || rule.getMaxRequests() <= 0 || rule.getWindowSeconds() <= 0) {
            return Mono.empty();
        }

        String key = "gateway:rate-limit:" + dimension + ":" + identifier.trim();
        return redisTemplate.opsForValue().increment(key)
            .switchIfEmpty(Mono.error(new GatewayException(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                50001,
                "rate limit counter failed"
            )))
            .flatMap(current -> {
                Mono<Boolean> expireMono = current == 1L
                    ? redisTemplate.expire(key, Duration.ofSeconds(rule.getWindowSeconds()))
                    : Mono.just(Boolean.TRUE);
                return expireMono.thenReturn(current);
            })
            .flatMap(current -> {
                if (current > rule.getMaxRequests()) {
                    return Mono.error(new GatewayException(
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                        42900,
                        dimension + " rate limit exceeded"
                    ));
                }
                return Mono.empty();
            });
    }
}
