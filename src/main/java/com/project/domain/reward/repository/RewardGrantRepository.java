package com.project.domain.reward.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.reward.entity.RewardGrant;
import com.project.domain.reward.enums.RewardGrantStatus;

public interface RewardGrantRepository extends JpaRepository<RewardGrant, Long> {

    @Query(
            value =
                    "SELECT rg FROM RewardGrant rg "
                            + "JOIN FETCH rg.reward r "
                            + "JOIN FETCH rg.customer c "
                            + "JOIN FETCH rg.missionItem mi "
                            + "WHERE (:status IS NULL OR rg.status = :status) "
                            + "AND (:phoneNumber IS NULL OR c.phoneNumber LIKE %:phoneNumber%)",
            countQuery =
                    "SELECT COUNT(rg) FROM RewardGrant rg "
                            + "JOIN rg.customer c "
                            + "WHERE (:status IS NULL OR rg.status = :status) "
                            + "AND (:phoneNumber IS NULL OR c.phoneNumber LIKE %:phoneNumber%)")
    Page<RewardGrant> findWithFilters(
            @Param("status") RewardGrantStatus status,
            @Param("phoneNumber") String phoneNumber,
            Pageable pageable);

    @Query(
            value =
                    "SELECT rg FROM RewardGrant rg "
                            + "JOIN FETCH rg.reward r "
                            + "JOIN FETCH rg.customer c "
                            + "JOIN FETCH rg.missionItem mi "
                            + "WHERE (:status IS NULL OR rg.status = :status) "
                            + "AND (:phoneNumber IS NULL OR c.phoneNumber LIKE %:phoneNumber%) "
                            + "ORDER BY CASE WHEN rg.expiredAt IS NULL THEN 1 ELSE 0 END, rg.expiredAt ASC",
            countQuery =
                    "SELECT COUNT(rg) FROM RewardGrant rg "
                            + "JOIN rg.customer c "
                            + "WHERE (:status IS NULL OR rg.status = :status) "
                            + "AND (:phoneNumber IS NULL OR c.phoneNumber LIKE %:phoneNumber%)")
    Page<RewardGrant> findWithFiltersOrderByExpiringSoon(
            @Param("status") RewardGrantStatus status,
            @Param("phoneNumber") String phoneNumber,
            Pageable pageable);
}
