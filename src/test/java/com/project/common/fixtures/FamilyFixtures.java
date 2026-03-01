package com.project.common.fixtures;

import java.time.LocalDate;

import com.project.domain.family.entity.Family;

public class FamilyFixtures {

    public static final LocalDate CURRENT_MONTH = LocalDate.now().withDayOfMonth(1);

    public static Family family(Long createdById) {
        return Family.builder()
                .name("다봄 가족")
                .createdById(createdById)
                .totalQuotaBytes(10_000L)
                .usedBytes(3_000L)
                .currentMonth(CURRENT_MONTH)
                .build();
    }
}
