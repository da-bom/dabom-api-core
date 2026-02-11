package com.project.family.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.family.application.repository.FamilyQueryRepository;
import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilySearchResponse;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.domain.GlobalErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 가족 그룹 서비스
 * - 비즈니스 로직 판단은 Core 계층(Family)에 위임
 * - 여기서는 트랜잭션 관리와 흐름 제어만 담당
 */
@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyQueryRepository familyQueryRepository;

    @Transactional(readOnly = true)
    public Page<FamilySearchResponse> searchFamilies(String keyword, Pageable pageable) {
        return familyQueryRepository.search(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public FamilyDetailResponse getFamilyDetail(Long familyId) {
        return familyQueryRepository
                .findDetailById(familyId)
                .orElseThrow(
                        () ->
                                new ApplicationException(
                                        GlobalErrorCode.INTERNAL_SERVER_ERROR)); // TODO: 전용 에러코드
    }
}
