package com.project.domain.usagerecord.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.usagerecord.entity.UsageRecord;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    long countByEventTimeGreaterThanEqualAndDeletedAtIsNull(LocalDateTime from);
}
