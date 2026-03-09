package com.project.domain.appeal.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.appeal.entity.PolicyAppeal;
import com.project.domain.appeal.enums.AppealStatus;

/** 이의제기 목록 조회 저장소 */
public interface PolicyAppealRepository extends JpaRepository<PolicyAppeal, Long> {

    /** 가족 기준 이의제기 목록 조회 */
    @Query(
            """
            select pa
            from PolicyAppeal pa
            join fetch pa.requester requester
            left join fetch pa.policyAssignment policyAssignment
            where exists (
                select 1
                from FamilyMember fm
                where fm.customerId = requester.id
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
            join fetch pa.requester requester
            left join fetch pa.policyAssignment policyAssignment
            where requester.id = :requesterId
              and exists (
                select 1
                from FamilyMember fm
                where fm.customerId = requester.id
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
}
