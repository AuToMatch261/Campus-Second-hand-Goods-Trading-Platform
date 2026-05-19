package com.campus.gateway.filter;

import com.campus.gateway.config.GatewayAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_AUTH = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_NAME = "X-User-Name";

    private final GatewayAuthProperties props;
    private final AntPathMatcher matcher = new AntPathMatcher();

    private SecretKey signingKey;

    private SecretKey getSigningKey() {
        if (signingKey == null) {
            byte[] bytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("gateway.auth.secret 长度必须 >= 32 字节");
            }
            signingKey = Keys.hmacShaKeyFor(bytes);
        }
        return signingKey;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 拒绝外部访问任何服务的内部接口（仅供 Feign 跨服务调用）
        if (path.contains("/internal/")) {
            return notFound(exchange);
        }

        if (isWhitelisted(path, props.getWhitelist())) {
            return chain.filter(exchange);
        }

        String auth = request.getHeaders().getFirst(HEADER_AUTH);
        if (auth == null || !auth.startsWith(BEARER)) {
            return unauthorized(exchange, "缺少 Authorization 头");
        }

        String token = auth.substring(BEARER.length()).trim();
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("JWT 解析失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return unauthorized(exchange, "Token 无效或已过期");
        }

        String userId = claims.getSubject();
        String username = claims.get("username", String.class);

        ServerHttpRequest mutated = request.mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_ID);
                    h.remove(HEADER_USER_NAME);
                    h.add(HEADER_USER_ID, userId);
                    if (username != null) h.add(HEADER_USER_NAME, username);
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhitelisted(String path, List<String> patterns) {
        for (String p : patterns) {
            if (matcher.match(p, path)) return true;
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\"}";
        DataBuffer buf = resp.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Mono.just(buf));
    }

    private Mono<Void> notFound(ServerWebExchange exchange) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.NOT_FOUND);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":404,\"message\":\"not found\"}";
        DataBuffer buf = resp.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Mono.just(buf));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
