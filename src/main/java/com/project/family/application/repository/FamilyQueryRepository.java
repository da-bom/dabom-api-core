package com.project.family.application.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;

import com.project.family.web.dto.request.FamilySearchRequest;
import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilySearchResponse;

/** 읽기 전용 포트 */
public interface FamilyQueryRepository {
    Page<FamilySearchResponse> search(FamilySearchRequest request);

    Optional<FamilyDetailResponse> findDetailById(Long familyId);
}
