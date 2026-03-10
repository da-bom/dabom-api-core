package com.project.domain.appeal.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.project.domain.appeal.enums.AppealType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.appeal.entity.PolicyAppeal;
import com.project.domain.appeal.enums.AppealStatus;

/** 이의제기 목록 조회 저장소 */
public interface PolicyAppealRepository extends JpaRepository<PolicyAppeal, Long> {

    Optional<PolicyAppeal> findByIdAndDeletedAtIsNull(Long id);

    /** 동일 정책 진행 중 이의제기 존재 여부 확인 */
    boolean existsByPolicyAssignmentIdAndRequesterIdAndTypeAndStatusAndDeletedAtIsNull(
            Long policyAssignmentId, Long requesterId, AppealType type, AppealStatus status);

    /** 가족 기준 이의제기 목록 조회 */
    @Query(
            """
            select pa
            from PolicyAppeal pa
            where exists (
                select 1
                from FamilyMember fm
                where fm.customerId = pa.requesterId
                  and fm.familyId = :familyId
            )
              and (:status is null or pa.status = :status)
              and (:cursorId is null or pa.id < :cursorId)
              and pa.deletedAt is null
            order by pa.id desc
            """)
    List<PolicyAppeal> findAllByFamilyId(
            @Param("familyId") Long familyId,
            @Param("status") AppealStatus status,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    /** 요청자 기준 이의제기 목록 조회 */
    @Query(
            """
            select pa
            from PolicyAppeal pa
            where pa.requesterId = :requesterId
              and exists (
                select 1
                from FamilyMember fm
                where fm.customerId = pa.requesterId
                  and fm.familyId = :familyId
            )
              and (:status is null or pa.status = :status)
              and (:cursorId is null or pa.id < :cursorId)
              and pa.deletedAt is null
            order by pa.id desc
            """)
    List<PolicyAppeal> findByRequesterIdAndFamilyId(
            @Param("requesterId") Long requesterId,
            @Param("familyId") Long familyId,
            @Param("status") AppealStatus status,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    /** 긴급 요청 월별 승인 이력 조회 */
    @Query(
            """
            select pa
            from PolicyAppeal pa
            where pa.requesterId = :requesterId
              and pa.type = :type
              and pa.status = :status
              and pa.createdAt between :from and :to
              and pa.deletedAt is null
            """)
    List<PolicyAppeal> findByRequesterIdAndTypeAndStatusAndCreatedAtBetween(
            @Param("requesterId") Long requesterId,
            @Param("type") AppealType type,
            @Param("status") AppealStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
