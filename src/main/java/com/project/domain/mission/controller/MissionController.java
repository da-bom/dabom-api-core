package com.project.domain.mission.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.api.response.ApiResponse;
import com.project.common.auth.aop.CustomerId;
import com.project.common.auth.aop.OwnerOnly;
import com.project.common.auth.model.AuthContext;
import com.project.common.auth.service.AuthContextService;
import com.project.domain.mission.dto.request.CreateMissionRequest;
import com.project.domain.mission.dto.response.CreateMissionResponse;
import com.project.domain.mission.dto.response.MissionListResponse;
import com.project.domain.mission.dto.response.MissionLogListResponse;
import com.project.domain.mission.dto.response.MissionRequestHistoryListResponse;
import com.project.domain.mission.dto.response.MissionRequestResponse;
import com.project.domain.mission.model.CreateMissionResult;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.model.MissionLogListResult;
import com.project.domain.mission.model.MissionRequestHistoryListResult;
import com.project.domain.mission.model.MissionRequestResult;
import com.project.domain.mission.service.MissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/** 미션 조회/생성/요청/취소 HTTP 입출력을 담당하는 컨트롤러. */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/missions")
@Tag(name = "Mission", description = "미션 API")
public class MissionController {

    private final MissionService missionService;
    private final AuthContextService authContextService;

    /** 역할별 미션 목록을 조회한다. */
    @GetMapping
    @Operation(summary = "미션 목록 조회")
    public ApiResponse<MissionListResponse> getMissions(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        AuthContext auth = authContextService.resolve(customerId);
        MissionListResult result = missionService.listMissions(auth, cursor, size);
        return ApiResponse.success(MissionListResponse.from(result));
    }

    /** 역할별 미션 로그를 커서 기반으로 조회한다. */
    @GetMapping("/logs")
    @Operation(summary = "미션 로그 조회")
    public ApiResponse<MissionLogListResponse> getMissionLogs(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        AuthContext auth = authContextService.resolve(customerId);
        MissionLogListResult result = missionService.listMissionLogs(auth, cursor, size);
        return ApiResponse.success(MissionLogListResponse.from(result));
    }

    /** 미션 완료 요청 이력을 MissionRequest 기준으로 조회한다. */
    @GetMapping("/history")
    @Operation(summary = "미션 요청 이력 조회")
    public ApiResponse<MissionRequestHistoryListResponse> getMissionRequestHistory(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        AuthContext auth = authContextService.resolve(customerId);
        MissionRequestHistoryListResult result =
                missionService.listMissionRequestHistory(auth, cursor, size);
        return ApiResponse.success(MissionRequestHistoryListResponse.from(result));
    }

    /** OWNER가 미션을 생성한다. */
    @OwnerOnly
    @PostMapping
    @Operation(summary = "미션 생성")
    public ApiResponse<CreateMissionResponse> createMission(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestBody @Valid CreateMissionRequest request) {
        AuthContext auth = authContextService.resolve(customerId);
        CreateMissionResult result = missionService.createMission(auth, request);
        return ApiResponse.created(CreateMissionResponse.from(result));
    }

    /** OWNER가 미션을 취소한다. */
    @OwnerOnly
    @DeleteMapping("/{missionId}")
    @Operation(summary = "미션 취소")
    public ApiResponse<Void> cancelMission(
            @Parameter(hidden = true) @CustomerId Long customerId, @PathVariable Long missionId) {
        AuthContext auth = authContextService.resolve(customerId);
        missionService.cancelMission(auth, missionId);
        return ApiResponse.success(null);
    }

    /** MEMBER가 본인 미션 완료 요청을 생성한다. */
    @PostMapping("/{missionId}/request")
    @Operation(summary = "미션 완료 요청")
    public ApiResponse<MissionRequestResponse> requestMissionApproval(
            @Parameter(hidden = true) @CustomerId Long customerId, @PathVariable Long missionId) {
        AuthContext auth = authContextService.resolve(customerId);
        MissionRequestResult result = missionService.requestMissionApproval(auth, missionId);
        return ApiResponse.success(MissionRequestResponse.from(result));
    }
}
