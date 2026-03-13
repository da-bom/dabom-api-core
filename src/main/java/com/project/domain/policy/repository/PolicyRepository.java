package com.project.domain.policy.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.policy.entity.Policy;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByIdAndDeletedAtIsNull(Long id);

    List<Policy> findAllByIdInAndDeletedAtIsNull(Set<Long> ids);

    Page<Policy> findAllByDeletedAtIsNull(Pageable pageable);
}
