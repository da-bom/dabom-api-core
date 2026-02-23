package com.project.domain.policy.dto.response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.project.domain.policy.enums.PolicyType;

public record FamilyPolicyResponse(Long familyId, List<CustomerInfo> customers) {

    public record FlatPolicyRow(
            Long familyId,
            Long customerId,
            String customerName,
            String phoneNumber,
            String role,
            Long usedBytes,
            Long assignmentId,
            Long policyId,
            String policyName,
            PolicyType type,
            Boolean isActive,
            String rules) {}

    public static FamilyPolicyResponse from(List<FlatPolicyRow> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new FamilyPolicyResponse(null, List.of());
        }

        Long familyId = dtos.get(0).familyId();

        Map<Long, List<FlatPolicyRow>> grouped =
                dtos.stream().collect(Collectors.groupingBy(FlatPolicyRow::customerId));

        List<CustomerInfo> customerInfos =
                grouped.values().stream()
                        .map(
                                list -> {
                                    FlatPolicyRow first = list.get(0);

                                    List<PolicyInfo> policies =
                                            list.stream()
                                                    .filter(dto -> dto.assignmentId() != null)
                                                    .map(PolicyInfo::from)
                                                    .toList();

                                    return new CustomerInfo(
                                            first.customerId(),
                                            first.customerName(),
                                            first.phoneNumber(),
                                            first.role(),
                                            first.usedBytes() != null ? first.usedBytes() : 0L,
                                            policies);
                                })
                        .toList();

        return new FamilyPolicyResponse(familyId, customerInfos);
    }

    public record CustomerInfo(
            Long customerId,
            String name,
            String phoneNumber,
            String role,
            Long usedBytes,
            List<PolicyInfo> policies) {}

    public record PolicyInfo(
            Long assignmentId,
            Long policyId,
            String policyName,
            PolicyType type,
            boolean isActive,
            @JsonRawValue String rules) {
        public static PolicyInfo from(FlatPolicyRow dto) {
            return new PolicyInfo(
                    dto.assignmentId(),
                    dto.policyId(),
                    dto.policyName(),
                    dto.type(),
                    dto.isActive(),
                    dto.rules());
        }
    }
}
