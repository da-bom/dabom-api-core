package com.project.family.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.family.application.FamilyService;
import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilySearchResponse;
import com.project.global.api.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/** 가족 그룹 웹 어댑터 - 모든 응답을 SuccessResponse로 감싸서 반환 */
@RestController
@RequestMapping("/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FamilySearchResponse>>> searchFamilies(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<FamilySearchResponse> result = familyService.searchFamilies(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{familyId}")
    public ResponseEntity<ApiResponse<FamilyDetailResponse>> getFamilyDetail(
            @PathVariable Long familyId) {
        FamilyDetailResponse result = familyService.getFamilyDetail(familyId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
