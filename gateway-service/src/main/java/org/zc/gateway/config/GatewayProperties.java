package org.zc.gateway.config;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private Auth auth = new Auth();
    private Jwt jwt = new Jwt();
    private Blacklist blacklist = new Blacklist();
    private RateLimit rateLimit = new RateLimit();
    private Routes routes = new Routes();

    @Data
    public static class Auth {
        private String username = "admin";
        private String password = "admin123";
    }

    @Data
    public static class Jwt {
        private String secret = "change-this-jwt-secret-key-to-at-least-32-bytes";
        private long expireSeconds = 3600;
    }

    @Data
    public static class Blacklist {
        private List<String> deviceIds = new ArrayList<>();
        private List<String> ips = new ArrayList<>();
    }

    @Data
    public static class RateLimit {
        private Rule device = new Rule(60, 60);
        private Rule ip = new Rule(120, 60);
    }

    @Data
    public static class Routes {
        private String ingestUri = "http://localhost:8081";
    }

    @Data
    @NoArgsConstructor
    public static class Rule {
        private long maxRequests;
        private long windowSeconds;

        public Rule(long maxRequests, long windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
