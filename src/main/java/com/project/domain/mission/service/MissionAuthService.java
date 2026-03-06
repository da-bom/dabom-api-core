package com.project.domain.mission.service;

import org.springframework.stereotype.Component;

import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.mission.model.AuthContext;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;

import lombok.RequiredArgsConstructor;

/** customerId 기준으로 미션 도메인용 AuthContext를 구성한다. */
@Component
@RequiredArgsConstructor
public class MissionAuthService {

    private final FamilyMemberRepository familyMemberRepository;

    /** 요청 사용자 정보를 family/role과 함께 AuthContext로 변환한다. */
    public AuthContext resolve(Long customerId) {
        FamilyMember member =
                familyMemberRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));
        return new AuthContext(customerId, member.getFamilyId(), member.getRole());
    }
}
