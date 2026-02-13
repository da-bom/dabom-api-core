package com.project.family.web;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.admin.web.aop.AdminOnly;
import com.project.family.application.FamilyService;
import com.project.family.web.dto.request.FamilySearchRequest;
import com.project.family.web.dto.response.FamilyDetailResponse;
import com.project.family.web.dto.response.FamilySearchResponse;
import com.project.global.api.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @PostMapping
    @AdminOnly
    public ApiResponse<Page<FamilySearchResponse>> searchFamilies(
            @RequestBody FamilySearchRequest familySearchRequest) {
        Page<FamilySearchResponse> result = familyService.searchFamilies(familySearchRequest);
        return ApiResponse.success(result);
    }

    @GetMapping("/{familyId}")
    @AdminOnly
    public ApiResponse<FamilyDetailResponse> getFamilyDetail(@PathVariable Long familyId) {
        FamilyDetailResponse result = familyService.getFamilyDetail(familyId);
        return ApiResponse.success(result);
    }
}
