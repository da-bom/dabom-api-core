package com.project.family.infra.cache.dto;

import java.time.LocalDate;
import java.util.List;

import com.project.customer.core.Role;
import com.project.family.core.Family;
import com.project.family.core.FamilyMember;

public record FamilyCacheDto(
        Long id,
        String name,
        Long createdById,
        Long totalQuotaBytes,
        Long usedBytes,
        LocalDate currentMonth,
        List<FamilyMemberCacheDto> members) {

    public static FamilyCacheDto from(Family family) {
        return new FamilyCacheDto(
                family.getId(),
                family.getName(),
                family.getCreatedById(),
                family.getTotalQuotaBytes(),
                family.getUsedBytes(),
                family.getCurrentMonth(),
                family.getMembers().stream().map(FamilyMemberCacheDto::from).toList());
    }

    public Family toDomain() {
        return Family.builder()
                .id(id)
                .name(name)
                .createdById(createdById)
                .totalQuotaBytes(totalQuotaBytes)
                .usedBytes(usedBytes)
                .currentMonth(currentMonth)
                .members(
                        members != null
                                ? members.stream().map(FamilyMemberCacheDto::toDomain).toList()
                                : List.of())
                .build();
    }

    public record FamilyMemberCacheDto(Long id, Long familyId, Long customerId, Role role) {

        public static FamilyMemberCacheDto from(FamilyMember member) {
            return new FamilyMemberCacheDto(
                    member.getId(), member.getFamilyId(), member.getCustomerId(), member.getRole());
        }

        public FamilyMember toDomain() {
            return FamilyMember.builder()
                    .id(id)
                    .familyId(familyId)
                    .customerId(customerId)
                    .role(role)
                    .build();
        }
    }
}
