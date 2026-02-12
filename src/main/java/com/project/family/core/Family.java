package com.project.family.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/** Family 도메인 엔티티 (Aggregate Root) - 순수 POJO: JPA 어노테이션 없음 - 비즈니스 로직 응집: 상태 변경은 메서드로만 수행 */
@Getter
public class Family {

    private final Long id;
    private final String name;
    private final Long createdById;
    private final Long totalQuotaBytes;
    private final Long usedBytes;
    private final LocalDate currentMonth;
    private final List<FamilyMember> members;

    @Builder
    private Family(
            Long id,
            String name,
            Long createdById,
            Long totalQuotaBytes,
            Long usedBytes,
            LocalDate currentMonth,
            List<FamilyMember> members) {
        this.id = id;
        this.name = name;
        this.createdById = createdById;
        this.totalQuotaBytes = totalQuotaBytes;
        this.usedBytes = usedBytes;
        this.currentMonth = currentMonth;
        this.members = members != null ? members : new ArrayList<>();
    }

    /** 사용률 계산 비즈니스 로직 */
    public double calculateUsedPercent() {
        if (totalQuotaBytes == null || totalQuotaBytes == 0) {
            return 0.0;
        }
        return (double) usedBytes / totalQuotaBytes * 100.0;
    }

    /** 쿼터 수정 비즈니스 메서드 */
    public Family updateQuota(Long newTotalQuota) {
        FamilyRule.validateQuota(newTotalQuota);
        return Family.builder()
                .id(this.id)
                .name(this.name)
                .createdById(this.createdById)
                .totalQuotaBytes(newTotalQuota)
                .usedBytes(this.usedBytes)
                .currentMonth(this.currentMonth)
                .build();
    }
}
