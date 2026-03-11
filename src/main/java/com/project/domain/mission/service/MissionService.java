package com.project.domain.mission.service;

import com.project.domain.mission.dto.request.CreateMissionRequest;
import com.project.domain.mission.model.CreateMissionResult;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.model.MissionLogListResult;
import com.project.domain.mission.model.MissionRequestHistoryListResult;
import com.project.domain.mission.model.MissionRequestResult;
import com.project.global.auth.model.AuthContext;

/** 미션 도메인 비즈니스 로직 인터페이스다. */
public interface MissionService {
    MissionListResult listMissions(AuthContext auth, String cursor, int size);

    MissionLogListResult listMissionLogs(AuthContext auth, String cursor, int size);

    MissionRequestHistoryListResult listMissionRequestHistory(
            AuthContext auth, String cursor, int size);

    CreateMissionResult createMission(AuthContext auth, CreateMissionRequest req);

    void cancelMission(AuthContext auth, Long missionId);

    MissionRequestResult requestMissionApproval(AuthContext auth, Long missionId);
}
