package com.project.domain.mission.controller;

import com.project.domain.mission.dto.response.MissionResponse;
import com.project.domain.mission.enums.MissionStatus;
import com.project.global.api.response.ApiResponse;

public interface MissionController {
    ApiResponse<MissionResponse.ListResult> getMissions(Long customerId, MissionStatus status);
}
