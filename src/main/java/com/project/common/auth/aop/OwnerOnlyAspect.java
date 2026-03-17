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
import com.project.common.auth.enums.RoleType;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.CustomerErrorCode;

import io.jsonwebtoken.Claims;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnerOnlyAspect {

    private final JwtTokenUtil jwtTokenUtil;

    @Around("@annotation(com.project.common.auth.aop.OwnerOnly)")
    public Object validateOwner(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();

        String token = AuthorizationExtractor.extract(request);
        Claims claims = jwtTokenUtil.getVerifiedClaims(token);
        RoleType role = RoleType.valueOf(claims.get("role", String.class));

        if (role == RoleType.MEMBER) {
            throw new ApplicationException(CustomerErrorCode.CUSTOMER_FORBIDDEN);
        }

        return joinPoint.proceed();
    }
}
