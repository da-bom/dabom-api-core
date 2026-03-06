package com.project.domain.mission.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.mission.dto.response.MissionResponse;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.service.MissionService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.CustomerId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/missions")
@Tag(name = "Mission", description = "미션 조회 API")
public class MissionControllerImpl implements MissionController {

    private final MissionService missionService;

    @GetMapping
    @Operation(
            summary = "미션 목록 조회",
            description = "JWT 토큰의 고객 식별자를 기준으로 familyId를 추론해 미션 목록을 조회합니다.")
    @Override
    public ApiResponse<MissionResponse.ListResult> getMissions(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestParam(required = false) MissionStatus status) {
        MissionListResult missionListResult = missionService.getMissions(customerId, status);
        MissionResponse.ListResult response = MissionResponse.ListResult.from(missionListResult);
        return ApiResponse.success(response);
    }
}
