package com.project.domain.policy.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.policy.entity.Policy;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByIdAndDeletedAtIsNull(Long id);

    Page<Policy> findAllByDeletedAtIsNull(Pageable pageable);
}
