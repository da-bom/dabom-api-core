package com.project.policy.web.dto.response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.project.policy.application.dto.FamilyPolicyDto;
import com.project.policy.core.PolicyType;

public record FamilyPolicyResponse(Long familyId, List<CustomerInfo> customers) {
    public static FamilyPolicyResponse from(List<FamilyPolicyDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new FamilyPolicyResponse(null, List.of());
        }

        Long familyId = dtos.get(0).familyId();

        Map<Long, List<FamilyPolicyDto>> grouped =
                dtos.stream().collect(Collectors.groupingBy(FamilyPolicyDto::customerId));

        List<CustomerInfo> customerInfos =
                grouped.values().stream()
                        .map(
                                list -> {
                                    FamilyPolicyDto first = list.get(0);

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
                                            0L,
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
        public static PolicyInfo from(FamilyPolicyDto dto) {
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
