package com.project.common.auth.aop;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.project.common.auth.AuthorizationExtractor;
import com.project.common.auth.JwtTokenUtil;

import io.jsonwebtoken.Claims;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomerArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasMemberAnnotation = parameter.hasParameterAnnotation(CustomerId.class);
        boolean hasMemberType = Long.class.isAssignableFrom(parameter.getParameterType());

        return hasMemberAnnotation && hasMemberType;
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
        return Long.parseLong(claims.getSubject());
    }
}
