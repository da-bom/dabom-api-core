package com.project.domain.mission.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.domain.mission.entity.MissionItem;
import com.project.domain.mission.enums.MissionStatus;

/** MissionItem DB 접근 전용 Repository다. */
public interface MissionItemRepository extends JpaRepository<MissionItem, Long> {
    Optional<MissionItem> findByIdAndFamilyId(Long missionId, Long familyId);

    List<MissionItem> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    List<MissionItem> findByFamilyIdAndStatusOrderByCreatedAtDesc(
            Long familyId, MissionStatus status);

    List<MissionItem> findByTargetCustomerIdOrderByCreatedAtDesc(Long targetCustomerId);

    List<MissionItem> findByTargetCustomerIdAndStatusOrderByCreatedAtDesc(
            Long targetCustomerId, MissionStatus status);

    @Query(
            """
            select mi
            from MissionItem mi
            where mi.familyId = :familyId
              and (:status is null or mi.status = :status)
              and (:cursorId is null or mi.id < :cursorId)
            order by mi.id desc
            """)
    List<MissionItem> findByFamilyScope(
            Long familyId, MissionStatus status, Long cursorId, Pageable pageable);

    @Query(
            """
            select mi
            from MissionItem mi
            where mi.targetCustomerId = :targetCustomerId
              and (:status is null or mi.status = :status)
              and (:cursorId is null or mi.id < :cursorId)
            order by mi.id desc
            """)
    List<MissionItem> findByTargetScope(
            Long targetCustomerId, MissionStatus status, Long cursorId, Pageable pageable);
}
