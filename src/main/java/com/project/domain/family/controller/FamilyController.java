package com.project.domain.family.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.domain.family.dto.request.FamilySearchRequest;
import com.project.domain.family.dto.response.FamilyDetailResponse;
import com.project.domain.family.dto.response.FamilySearchResponse;
import com.project.domain.family.service.FamilyService;
import com.project.domain.usagerecord.UsageRecordService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.AdminOnly;
import com.project.global.auth.aop.CustomerId;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final UsageRecordService usageRecordService;

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

    @GetMapping(value = "reports/usage/current", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getCurrentUsage(@CustomerId Long customerId) {
        return usageRecordService.subscribe(customerId);
    }
}
