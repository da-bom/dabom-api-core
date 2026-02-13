package com.project.policy.web.dto.response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.project.policy.application.dto.FamilyPolicyDto;
import com.project.policy.core.PolicyType;

public record FamilyPolicyResponse(Long familyId, List<CustomerInfo> customers) {
    // Flat DTO -> 계층형 응답 변환 팩토리 메서드
    public static FamilyPolicyResponse from(List<FamilyPolicyDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new FamilyPolicyResponse(null, List.of());
        }

        Long familyId = dtos.get(0).getFamilyId();

        Map<Long, List<FamilyPolicyDto>> grouped =
                dtos.stream().collect(Collectors.groupingBy(FamilyPolicyDto::getCustomerId));

        List<CustomerInfo> customerInfos =
                grouped.values().stream()
                        .map(
                                list -> {
                                    FamilyPolicyDto first = list.get(0);

                                    List<PolicyInfo> policies =
                                            list.stream()
                                                    .filter(dto -> dto.getAssignmentId() != null)
                                                    .map(PolicyInfo::from)
                                                    .toList();

                                    return new CustomerInfo(
                                            first.getCustomerId(),
                                            first.getCustomerName(),
                                            first.getPhoneNumber(),
                                            first.getRole(),
                                            0L, // 사용량 정보
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
                    dto.getAssignmentId(),
                    dto.getPolicyId(),
                    dto.getPolicyName(),
                    dto.getType(),
                    dto.isActive(),
                    dto.getRules());
        }
    }
}
