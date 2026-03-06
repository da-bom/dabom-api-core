package com.project.domain.mission.service;

import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.model.MissionListResult;

public interface MissionService {
    MissionListResult getMissions(Long customerId, MissionStatus status);
}
