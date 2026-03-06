package com.project.domain.mission.repository;

import java.util.List;

import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.repository.projection.MissionListRow;

public interface MissionItemRepositoryCustom {
    List<MissionListRow> findMissionRowsByFamilyIdAndStatus(Long familyId, MissionStatus status);
}
