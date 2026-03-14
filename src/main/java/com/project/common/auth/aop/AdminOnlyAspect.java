package com.project.common.auth.aop;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.project.common.auth.AuthorizationExtractor;
import com.project.common.auth.JwtTokenUtil;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.AdminErrorCode;
import com.project.domain.customer.enums.RoleType;

import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminOnlyAspect {
    private final JwtTokenUtil jwtTokenUtil;

    @Around("@annotation(com.project.common.auth.aop.AdminOnly)")
    public Object validateAdmin(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();

        String token = AuthorizationExtractor.extract(request);
        if (token == null || token.isBlank()) {
            throw new ApplicationException(AdminErrorCode.ADMIN_UNAUTHORIZED);
        }

        RoleType role;
        try {
            role = jwtTokenUtil.getRole(token);
        } catch (JwtException e) {
            throw new ApplicationException(AdminErrorCode.ADMIN_UNAUTHORIZED);
        }

        if (role != RoleType.ADMIN) {
            throw new ApplicationException(AdminErrorCode.ADMIN_FORBIDDEN);
        }

        return joinPoint.proceed();
    }
}
