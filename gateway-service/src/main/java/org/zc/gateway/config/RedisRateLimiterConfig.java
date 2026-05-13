package org.zc.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;


/**
 * 多组合限流 key = deviceId + ":" + api + ":" + ip
 */
@Configuration
public class RedisRateLimiterConfig {


    @Bean
    public KeyResolver globalKeyResolver(){
        return exchange -> Mono.just("GLOBAL");
    }

    /**
     * 用于防攻击（DDOS），不是核心限流
     * @return
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange ->
                Mono.just(Objects.requireNonNull(
                                exchange.getRequest()
                                        .getRemoteAddress())
                        .getAddress()
                        .getHostAddress()
                );
    }

    /**
     * 按照设备id
     * @return
     */
    @Bean
    public KeyResolver deviceKeyResolver() {
        return exchange -> {
            String deviceId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("deviceId");
            return Mono.just(deviceId != null ? deviceId : "unknown");
        };
    }

    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange ->
                Mono.just(exchange.getRequest().getPath().value());
    }
}
