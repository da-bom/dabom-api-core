package com.project.common.auth.service;

import org.springframework.stereotype.Component;

import com.project.common.auth.model.AuthContext;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.CustomerErrorCode;
import com.project.common.exception.code.FamilyErrorCode;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;

import lombok.RequiredArgsConstructor;

/** customerId를 기반으로 여러 도메인에서 공통으로 사용할 인증 컨텍스트(AuthContext)를 생성합니다. */
@Component
@RequiredArgsConstructor
public class AuthContextService {

    private final FamilyMemberRepository familyMemberRepository;
    private final CustomerRepository customerRepository;

    public AuthContext resolve(Long customerId) {
        FamilyMember member =
                familyMemberRepository
                        .findByCustomerIdAndDeletedAtIsNull(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));
        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                CustomerErrorCode.CUSTOMER_NOT_FOUND));
        return new AuthContext(
                customerId, member.getFamilyId(), member.getRole(), customer.getName());
    }

    // 고객의 존재 여부와 가족 구성원 여부만 확인
    public void verifyUserAndFamilyMembership(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ApplicationException(CustomerErrorCode.CUSTOMER_NOT_FOUND);
        }
        if (!familyMemberRepository.existsByCustomerIdAndDeletedAtIsNull(customerId)) {
            throw new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND);
        }
    }
}
