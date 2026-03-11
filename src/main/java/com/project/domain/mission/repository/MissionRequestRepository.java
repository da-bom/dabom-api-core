package com.project.domain.mission.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.enums.MissionRequestStatus;

/** MissionRequest DB 접근 전용 Repository */
public interface MissionRequestRepository extends JpaRepository<MissionRequest, Long> {

    boolean existsByMissionItemIdAndRequesterIdAndStatus(
            Long missionItemId, Long requesterId, MissionRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select mr from MissionRequest mr where mr.id = :requestId")
    Optional<MissionRequest> findByIdForUpdate(Long requestId);

    @Query(
            """
            select mr
            from MissionRequest mr
            join MissionItem mi on mi.id = mr.missionItemId
            where mi.familyId = :familyId
              and (:cursorId is null or mr.id < :cursorId)
            order by mr.id desc
            """)
    List<MissionRequest> findByFamilyIdOrderByIdDesc(Long familyId, Long cursorId, Pageable pageable);

    @Query(
            """
            select mr
            from MissionRequest mr
            where mr.requesterId = :requesterId
              and (:cursorId is null or mr.id < :cursorId)
            order by mr.id desc
            """)
    List<MissionRequest> findByRequesterIdOrderByIdDesc(
            Long requesterId, Long cursorId, Pageable pageable);

    @Query(
            """
            select mr
            from MissionRequest mr
            where mr.missionItemId in :missionItemIds
            order by mr.createdAt desc, mr.id desc
            """)
    List<MissionRequest> findByMissionItemIdInOrderByCreatedAtDescIdDesc(Set<Long> missionItemIds);

    @Query(
            """
            select mr
            from MissionRequest mr
            where mr.requesterId = :requesterId
              and mr.status = MissionRequestStatus.APPROVED
              and mr.resolvedAt is not null
              and (:cursorId is null or mr.id < :cursorId)
            order by mr.id desc
            """)
    List<MissionRequest> findApprovedByTargetCustomerIdOrderByResolvedAtDesc(
            Long requesterId, Long cursorId, Pageable pageable);
}
