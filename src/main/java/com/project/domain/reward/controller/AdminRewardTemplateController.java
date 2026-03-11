package com.project.domain.reward.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.reward.dto.request.RewardTemplateRequest;
import com.project.domain.reward.dto.response.RewardTemplateResponse;
import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.service.RewardTemplateService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.AdminOnly;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/rewards/templates")
@RequiredArgsConstructor
@Tag(name = "Admin_Reward", description = "Admin 전용 보상 템플릿 API")
public class AdminRewardTemplateController {

    private final RewardTemplateService rewardTemplateService;

    @GetMapping
    @AdminOnly
    @Operation(summary = "보상 템플릿 목록 조회", description = "전체 보상 템플릿을 조회합니다.")
    public ApiResponse<List<RewardTemplateResponse.Detail>> getAllTemplates() {
        List<RewardTemplateResponse.Detail> result =
                rewardTemplateService.getAllTemplates().stream()
                        .map(RewardTemplateResponse.Detail::from)
                        .toList();
        return ApiResponse.success(result);
    }

    @PostMapping
    @AdminOnly
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "보상 템플릿 생성", description = "새로운 보상 템플릿을 생성합니다.")
    public ApiResponse<RewardTemplateResponse.Detail> createTemplate(
            @Valid @RequestBody RewardTemplateRequest.Create request) {
        RewardTemplate template = rewardTemplateService.createTemplate(request);
        return ApiResponse.created(RewardTemplateResponse.Detail.from(template));
    }

    @PutMapping("/{id}")
    @AdminOnly
    @Operation(summary = "보상 템플릿 수정", description = "보상 템플릿을 수정합니다.")
    @Parameter(name = "id", description = "보상 템플릿 ID", required = true)
    public ApiResponse<RewardTemplateResponse.Detail> updateTemplate(
            @PathVariable Long id, @Valid @RequestBody RewardTemplateRequest.Update request) {
        RewardTemplate template = rewardTemplateService.updateTemplate(id, request);
        return ApiResponse.success(RewardTemplateResponse.Detail.from(template));
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    @Operation(summary = "보상 템플릿 삭제", description = "보상 템플릿을 삭제합니다. (Soft Delete)")
    @Parameter(name = "id", description = "보상 템플릿 ID", required = true)
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        rewardTemplateService.deleteTemplate(id);
        return ApiResponse.success(null);
    }
}
