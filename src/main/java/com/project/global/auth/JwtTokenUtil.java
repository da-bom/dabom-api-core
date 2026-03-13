package com.project.global.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.domain.customer.enums.RoleType;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.GlobalErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenUtil {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${spring.jwt.token.access-expiration-time}")
    private Long accessExpirationMillis;

    @Value("${spring.jwt.token.refresh-expiration-time}")
    private Long refreshExpirationMillis;

    @Value("${spring.jwt.secret-key}")
    private String secretKey;

    private SecretKey key;

    @PostConstruct
    public void initialize() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long memberId, RoleType role) {
        return generateToken(memberId.toString(), role, accessExpirationMillis, TOKEN_TYPE_ACCESS);
    }

    public String createRefreshToken(Long memberId, RoleType role) {
        return generateToken(
                memberId.toString(), role, refreshExpirationMillis, TOKEN_TYPE_REFRESH);
    }

    private String generateToken(String subject, RoleType role, Long expirationTime, String type) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);
        try {
            return Jwts.builder()
                    .setSubject(subject)
                    .claim("role", role.name())
                    .claim("type", type)
                    .setExpiration(expiration)
                    .signWith(key)
                    .compact();
        } catch (JwtException e) {
            throw new JwtException(String.format("%s 토큰 생성 중 오류가 발생했습니다.", type));
        }
    }

    public Claims verify(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            throw new JwtException("유효기간이 만료된 토큰입니다");
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("유효하지 않은 토큰입니다");
        }
    }

    public Long getMemberId(String token) {
        try {
            return Long.parseLong(verifyAccessToken(token).getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("유효하지 않은 토큰 subject입니다");
        }
    }

    public RoleType getRole(String token) {
        String roleStr = verifyAccessToken(token).get("role", String.class);
        return RoleType.valueOf(roleStr);
    }

    public Claims getVerifiedClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED_TOKEN);
        }
        try {
            return verifyAccessToken(token);
        } catch (JwtException e) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED_TOKEN);
        }
    }

    public Claims verifyAccessToken(String token) {
        Claims claims = verify(token);
        String type = claims.get("type", String.class);
        if (!TOKEN_TYPE_ACCESS.equals(type)) {
            throw new JwtException("액세스 토큰이 아닙니다.");
        }
        return claims;
    }

    public Claims verifyRefreshToken(String token) {
        Claims claims = verify(token);
        String type = claims.get("type", String.class);
        if (!TOKEN_TYPE_REFRESH.equals(type)) {
            throw new JwtException("리프레시 토큰이 아닙니다.");
        }
        return claims;
    }

    public TokenRefreshResult reissueTokens(Long memberId, RoleType role) {
        String accessToken = createToken(memberId, role);
        String refreshToken = createRefreshToken(memberId, role);
        long expiresIn = accessExpirationMillis / 1000;
        return new TokenRefreshResult(accessToken, refreshToken, expiresIn);
    }
}
