package org.zc.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zc.common.Result;
import org.zc.gateway.dto.LoginRequest;
import org.zc.gateway.dto.LoginResponse;
import org.zc.gateway.filter.GatewayAuthenticationFilter;
import org.zc.gateway.service.AuthService;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Mono<Result<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return Mono.fromSupplier(() -> Result.ok(authService.login(request)));
    }

    @GetMapping("/me")
    public Mono<Result<Map<String, String>>> currentUser(
        @RequestAttribute(name = GatewayAuthenticationFilter.AUTHENTICATED_USER_ATTR, required = false) String username
    ) {
        return Mono.just(Result.ok(Map.of("username", username == null ? "" : username)));
    }
}
