package com.project.domain.customer.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.customer.entity.CustomerQuota;

public interface CustomerQuotaRepository extends JpaRepository<CustomerQuota, Long> {
    Optional<CustomerQuota> findByFamilyIdAndCustomerIdAndCurrentMonthAndDeletedAtIsNull(
            Long familyId, Long customerId, LocalDate currentMonth);
}
