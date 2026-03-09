package com.project.global.auth.service;

import org.springframework.stereotype.Component;

import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.global.auth.model.AuthContext;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * customerId를 기반으로 여러 도메인에서 공통으로 사용할 인증 컨텍스트(AuthContext)를 생성합니다.
 */
@Component
@RequiredArgsConstructor
public class AuthContextService {

    private final FamilyMemberRepository familyMemberRepository;

    public AuthContext resolve(Long customerId) {
        FamilyMember member =
                familyMemberRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));
        return new AuthContext(customerId, member.getFamilyId(), member.getRole());
    }
}
