package com.project.global.auth.aop;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.project.domain.customer.enums.RoleType;
import com.project.global.auth.AuthorizationExtractor;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.CustomerErrorCode;
import com.project.global.exception.code.GlobalErrorCode;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnerOnlyAspect {

    private final JwtTokenUtil jwtTokenUtil;

    @Around("@annotation(com.project.global.auth.aop.OwnerOnly)")
    public Object validateOwner(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();

        String token = AuthorizationExtractor.extract(request);
        if (token == null || token.isBlank()) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED_TOKEN);
        }

        RoleType role;
        try {
            role = jwtTokenUtil.getRole(token);
        } catch (RuntimeException e) {
            throw new ApplicationException(GlobalErrorCode.UNAUTHORIZED_TOKEN);
        }

        if (role == RoleType.MEMBER) {
            throw new ApplicationException(CustomerErrorCode.CUSTOMER_FORBIDDEN);
        }

        return joinPoint.proceed();
    }
}
