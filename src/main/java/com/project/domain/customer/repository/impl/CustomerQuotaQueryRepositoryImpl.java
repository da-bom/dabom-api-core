package com.project.domain.customer.repository.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.project.domain.customer.entity.QCustomer;
import com.project.domain.customer.entity.QCustomerQuota;
import com.project.domain.customer.repository.CustomerQuotaQueryRepository;
import com.project.domain.family.entity.QFamilyMember;
import com.project.domain.usagerecord.dto.response.CustomerUsageResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class CustomerQuotaQueryRepositoryImpl implements CustomerQuotaQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CustomerUsageResponse> findCustomerUsage(Long familyId, Long customerId) {

        QCustomerQuota customerQuota = QCustomerQuota.customerQuota;
        QFamilyMember familyMember = QFamilyMember.familyMember;
        QCustomer customer = QCustomer.customer;

        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);

        return queryFactory
                .select(
                        Projections.constructor(
                                CustomerUsageResponse.class,
                                customerQuota.customerId,
                                customer.name,
                                customerQuota.monthlyUsedBytes,
                                customerQuota.monthlyLimitBytes,
                                customerQuota.customerId.eq(customerId)))
                .from(customerQuota)
                .join(familyMember)
                .on(customerQuota.customerId.eq(familyMember.customerId))
                .join(customer)
                .on(familyMember.customerId.eq(customer.id))
                .where(
                        customerQuota.familyId.eq(familyId),
                        familyMember.familyId.eq(familyId),
                        customerQuota.currentMonth.eq(currentMonth))
                .fetch();
    }
}
