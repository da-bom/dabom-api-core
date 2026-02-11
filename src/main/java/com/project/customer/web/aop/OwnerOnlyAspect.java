package com.project.customer.web.aop;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.project.customer.application.AuthorizationExtractor;
import com.project.customer.application.JwtTokenUtil;
import com.project.customer.core.Role;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnerOnlyAspect {

    private final JwtTokenUtil jwtTokenUtil;

    @Around("@annotation(com.project.customer.web.aop.OwnerOnly)")
    public Object validateOwner(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();

        String token = AuthorizationExtractor.extract(request);

        Role role = jwtTokenUtil.getRole(token);

        if (role == Role.MEMBER) {
            throw new IllegalArgumentException("OWNER부터 권한만 접근 가능합니다");
        }

        return joinPoint.proceed();
    }
}
