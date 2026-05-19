package com.campus.common.security;

import com.campus.common.exception.BusinessException;
import com.campus.common.response.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_USERNAME = "username";

    private final JwtProperties props;

    private SecretKey signingKey() {
        byte[] bytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret 长度必须 >= 32 字节");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String issue(long userId, String username) {
        long now = System.currentTimeMillis();
        Date exp = new Date(now + props.getExpireMinutes() * 60_000L);
        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .issuedAt(new Date(now))
                .expiration(exp)
                .signWith(signingKey())
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token 无效或已过期");
        }
    }

    public long getUserId(String token) {
        return Long.parseLong(parse(token).getSubject());
    }

    public String getUsername(String token) {
        return parse(token).get(CLAIM_USERNAME, String.class);
    }
}
