package com.project.family.application.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilySearchResponse;

/** 읽기 전용 포트 (DTO 반환) */
public interface FamilyQueryRepository {
    Page<FamilySearchResponse> search(String keyword, Pageable pageable);

    Optional<FamilyDetailResponse> findDetailById(Long familyId);
}
