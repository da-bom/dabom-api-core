package com.project.domain.mission.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.enums.MissionRequestStatus;

/** MissionRequest DB 접근 전용 Repository */
public interface MissionRequestRepository extends JpaRepository<MissionRequest, Long> {

    boolean existsByMissionItemIdAndRequesterIdAndStatus(
            Long missionItemId, Long requesterId, MissionRequestStatus status);

    @Query(
            """
            select mr
            from MissionRequest mr
            where mr.requesterId = :requesterId
              and mr.status = com.project.domain.mission.enums.MissionRequestStatus.APPROVED
              and mr.resolvedAt is not null
              and (:cursorId is null or mr.id < :cursorId)
            order by mr.id desc
            """)
    List<MissionRequest> findApprovedByTargetCustomerIdOrderByResolvedAtDesc(
            Long requesterId, Long cursorId, Pageable pageable);
}
