package com.project.admin.web.aop;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.project.customer.core.Role;
import com.project.global.auth.AuthorizationExtractor;
import com.project.global.auth.JwtTokenUtil;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminOnlyAspect {
    private final JwtTokenUtil jwtTokenUtil;

    @Around("@annotation(com.project.admin.web.aop.AdminOnly)")
    public Object validateAdmin(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();

        String token = AuthorizationExtractor.extract(request);

        Role role = jwtTokenUtil.getRole(token);

        if (role != Role.ADMIN) {
            throw new IllegalArgumentException("관리자만 접근 가능합니다.");
        }

        return joinPoint.proceed();
    }
}
