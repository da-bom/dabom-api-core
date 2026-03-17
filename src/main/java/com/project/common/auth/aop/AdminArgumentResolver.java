package com.project.common.auth.aop;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.project.common.auth.AuthorizationExtractor;
import com.project.common.auth.JwtTokenUtil;
import com.project.common.auth.enums.RoleType;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.AdminErrorCode;

import io.jsonwebtoken.Claims;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdminArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(AdminId.class);
        boolean hasType = Long.class.isAssignableFrom(parameter.getParameterType());

        return hasAnnotation && hasType;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory)
            throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        String token = AuthorizationExtractor.extract(request);
        Claims claims = jwtTokenUtil.getVerifiedClaims(token);

        String role = claims.get("role", String.class);
        if (!RoleType.ADMIN.name().equals(role)) {
            throw new ApplicationException(AdminErrorCode.ADMIN_FORBIDDEN);
        }

        return Long.parseLong(claims.getSubject());
    }
}
