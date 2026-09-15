package com.rikkeibank.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Global JWT validation filter running at the API Gateway (single entry point).
 * - Validates the access token signature & expiry.
 * - Rejects a token if the user was force-logged-out by an ADMIN after the token was issued
 *   (checked against a Redis blacklist key set by identity-service -> immediate revocation,
 *   even though JWT itself is stateless).
 * - Forwards X-User-Id / X-User-Name / X-User-Role headers downstream so every microservice
 *   can apply @PreAuthorize without re-validating the token itself.
 */
@Component
public class JwtAuthenticationFilter implements WebFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/identity/auth/login",
            "/api/identity/auth/refresh",
            "/api/identity/auth/register",
            "/actuator",
            "/eureka"
    );

    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }
        String token = authHeader.substring(7);

        Claims claims;
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        String userId = String.valueOf(claims.get("userId"));
        Date issuedAt = claims.getIssuedAt();

        return redisTemplate.opsForValue().get("blacklist:" + username)
                .defaultIfEmpty("0")
                .flatMap(blacklistedAtStr -> {
                    long blacklistedAt = Long.parseLong(blacklistedAtStr);
                    if (blacklistedAt > 0 && issuedAt != null && issuedAt.getTime() < blacklistedAt) {
                        return unauthorized(exchange, "Access has been revoked by ADMIN. Please login again.");
                    }
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Name", username)
                            .header("X-User-Role", role)
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
