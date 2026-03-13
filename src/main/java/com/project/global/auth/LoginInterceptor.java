package com.project.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.GlobalErrorCode;

import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        final String token = AuthorizationExtractor.extract(request);
        if (token == null || token.isBlank()) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED_TOKEN);
        }
        try {
            jwtTokenUtil.verifyAccessToken(token);
        } catch (JwtException e) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED_TOKEN);
        }
        return true;
    }
}
