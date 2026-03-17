package com.project.domain.family.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.api.response.ApiResponse;
import com.project.common.auth.aop.AdminOnly;
import com.project.domain.family.dto.request.AdminFamilyUpdateRequest;
import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.dto.response.AdminFamilyUpdateResponse;
import com.project.domain.family.dto.response.FamilyDetailResponse;
import com.project.domain.family.dto.response.FamilySearchResponse;
import com.project.domain.family.model.FamilyDetail;
import com.project.domain.family.model.FamilySearchResult;
import com.project.domain.family.service.FamilyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/families")
@RequiredArgsConstructor
@Tag(name = "Admin_Family", description = "Admin 전용 가족 API")
public class AdminFamilyController {

    private final FamilyService familyService;

    @PostMapping
    @AdminOnly
    @Operation(summary = "가족 목록 검색", description = "페이지, 필터, 검색 조건으로 가족 목록을 조회합니다.")
    public ApiResponse<Page<FamilySearchResponse>> searchFamilies(
            @RequestBody FamilySearchRequest familySearchRequest) {
        Page<FamilySearchResult> searchResult = familyService.searchFamilies(familySearchRequest);
        Page<FamilySearchResponse> result = searchResult.map(FamilySearchResponse::from);
        return ApiResponse.success(result);
    }

    @GetMapping("/{familyId}")
    @AdminOnly
    @Operation(summary = "가족 상세 조회", description = "가족 ID로 가족 상세 정보를 조회합니다.")
    public ApiResponse<FamilyDetailResponse> getFamilyDetail(
            @Parameter(description = "가족 ID", required = true) @PathVariable Long familyId) {
        FamilyDetail familyDetail = familyService.getFamilyDetail(familyId);
        return ApiResponse.success(FamilyDetailResponse.from(familyDetail));
    }

    @PatchMapping("/{familyId}")
    @AdminOnly
    @Operation(summary = "가족 구성원 권한/한도 수정", description = "구성원의 역할과 데이터 한도를 일괄 수정합니다.")
    public ApiResponse<AdminFamilyUpdateResponse> updateFamily(
            @Parameter(description = "가족 ID", required = true) @PathVariable Long familyId,
            @Valid @RequestBody AdminFamilyUpdateRequest request) {
        int updatedCount = familyService.updateFamilyByAdmin(familyId, request.members());
        return ApiResponse.success(new AdminFamilyUpdateResponse(familyId, updatedCount));
    }
}
