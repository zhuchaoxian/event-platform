package org.zc.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.zc.common.Result;
import org.zc.gateway.exception.GatewayException;
import org.zc.gateway.service.BlacklistService;
import org.zc.gateway.service.GatewayRateLimitService;
import org.zc.gateway.service.JwtTokenService;
import org.zc.gateway.support.RequestIdentityResolver;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {
    public static final String AUTHENTICATED_USER_ATTR = "authenticatedUser";

    private final BlacklistService blacklistService;
    private final GatewayRateLimitService gatewayRateLimitService;
    private final JwtTokenService jwtTokenService;
    private final RequestIdentityResolver requestIdentityResolver;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String deviceId = requestIdentityResolver.resolveDeviceId(exchange.getRequest());
        String clientIp = requestIdentityResolver.resolveClientIp(exchange.getRequest());

        return Mono.defer(() -> {
                blacklistService.validate(clientIp, deviceId);
                return gatewayRateLimitService.validate(clientIp, deviceId);
            })
            .then(Mono.defer(() -> {
                if (isPublicEndpoint(exchange)) {
                    return chain.filter(exchange);
                }
                String token = extractBearerToken(exchange.getRequest().getHeaders());
                String username = jwtTokenService.parseUsername(token);
                exchange.getAttributes().put(AUTHENTICATED_USER_ATTR, username);
                return chain.filter(exchange);
            }))
            .onErrorResume(exception -> writeErrorResponse(exchange, exception));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicEndpoint(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        return "OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())
            || "/api/auth/login".equals(path)
                || path.startsWith("/api/ingest/")
            || path.startsWith("/error");
    }

    private String extractBearerToken(HttpHeaders headers) {
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new GatewayException(org.springframework.http.HttpStatus.UNAUTHORIZED, 40100, "missing bearer token");
        }
        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            throw new GatewayException(org.springframework.http.HttpStatus.UNAUTHORIZED, 40100, "missing bearer token");
        }
        return token;
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, Throwable throwable) {
        GatewayException gatewayException = throwable instanceof GatewayException
            ? (GatewayException) throwable
            : new GatewayException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, 50000, "internal server error");

        Result<Void> body = new Result<>();
        body.setCode(gatewayException.getCode());
        body.setMsg(gatewayException.getMessage());

        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(body);
        } catch (Exception exception) {
            payload = ("{\"code\":50000,\"msg\":\"internal server error\"}").getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(gatewayException.getStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }
}
