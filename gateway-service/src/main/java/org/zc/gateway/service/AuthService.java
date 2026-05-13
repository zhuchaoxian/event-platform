package org.zc.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.zc.gateway.config.GatewayProperties;
import org.zc.gateway.dto.LoginRequest;
import org.zc.gateway.dto.LoginResponse;
import org.zc.gateway.exception.GatewayException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final GatewayProperties gatewayProperties;
    private final JwtTokenService jwtTokenService;

    public LoginResponse login(LoginRequest request) {
        GatewayProperties.Auth auth = gatewayProperties.getAuth();
        if (!auth.getUsername().equals(request.getUsername()) || !auth.getPassword().equals(request.getPassword())) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, 40101, "invalid username or password");
        }
        return jwtTokenService.generateToken(request.getUsername());
    }
}
