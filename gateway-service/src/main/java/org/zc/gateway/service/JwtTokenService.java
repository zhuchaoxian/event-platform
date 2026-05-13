package org.zc.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.zc.gateway.config.GatewayProperties;
import org.zc.gateway.dto.LoginResponse;
import org.zc.gateway.exception.GatewayException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtTokenService {
    private final GatewayProperties gatewayProperties;

    public LoginResponse generateToken(String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(gatewayProperties.getJwt().getExpireSeconds());
        String token = Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiresAt))
            .signWith(secretKey(), SignatureAlgorithm.HS256)
            .compact();
        return new LoginResponse(token, expiresAt.toEpochMilli());
    }

    public String parseUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            return claims.getSubject();
        } catch (Exception exception) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, 40102, "invalid or expired jwt token");
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(gatewayProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
