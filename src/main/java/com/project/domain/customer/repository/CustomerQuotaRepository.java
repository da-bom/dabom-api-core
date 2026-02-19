package com.project.domain.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.customer.entity.CustomerQuota;

public interface CustomerQuotaRepository extends JpaRepository<CustomerQuota, Long> {}
