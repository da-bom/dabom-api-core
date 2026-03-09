package com.project.domain.mission.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.domain.mission.entity.MissionLog;

/** Mission 로그 조회/저장을 담당하는 Repository다. */
public interface MissionLogRepository extends JpaRepository<MissionLog, Long> {

    @Query(
            """
            select ml
            from MissionLog ml
            join MissionItem mi on ml.missionItemId = mi.id
            where mi.familyId = :familyId
              and (:cursorId is null or ml.id < :cursorId)
            order by ml.id desc
            """)
    List<MissionLog> findByFamilyScope(Long familyId, Long cursorId, Pageable pageable);

    @Query(
            """
            select ml
            from MissionLog ml
            join MissionItem mi on ml.missionItemId = mi.id
            where mi.targetCustomerId = :targetCustomerId
              and (:cursorId is null or ml.id < :cursorId)
            order by ml.id desc
            """)
    List<MissionLog> findByTargetScope(Long targetCustomerId, Long cursorId, Pageable pageable);
}
