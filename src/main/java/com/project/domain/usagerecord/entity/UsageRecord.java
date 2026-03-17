package com.project.domain.usagerecord.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.project.common.util.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usage_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 50, unique = true)
    private String eventId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "bytes_used", nullable = false)
    private Long bytesUsed;

    @Column(name = "app_id", length = 100)
    private String appId;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Builder
    public UsageRecord(
            String eventId,
            Long customerId,
            Long familyId,
            Long bytesUsed,
            String appId,
            LocalDateTime eventTime) {
        this.eventId = eventId;
        this.customerId = customerId;
        this.familyId = familyId;
        this.bytesUsed = bytesUsed;
        this.appId = appId;
        this.eventTime = eventTime;
    }
}
