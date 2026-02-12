package com.project.family.web;

import com.project.family.core.FamilyMember;
import com.project.family.web.dto.response.FamilyMemberDetailResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** FamilyMember Web 계층 변환 Mapper */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FamilyMemberWebMapper {

    /** Domain -> FamilyMemberDetailResponse */
    public static FamilyMemberDetailResponse toDetailResponse(FamilyMember domain) {
        return new FamilyMemberDetailResponse(
                domain.getCustomerId(),
                null, // name (도메인 또는 별도 조회를 통해 채움)
                domain.getRole(),
                null, // monthlyLimitBytes
                null // monthlyUsedBytes
                );
    }
}
