package com.project.customer.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.customer.infra.entity.CustomerJpaEntity;

public interface CustomerJpaEntityRepository extends JpaRepository<CustomerJpaEntity, Long> {
    CustomerJpaEntity findByPhoneNumber(String phoneNumber);
}
