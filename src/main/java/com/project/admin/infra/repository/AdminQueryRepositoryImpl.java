package com.project.admin.infra.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.project.admin.application.repository.AdminQueryRepository;
import com.project.admin.core.Admin;
import com.project.admin.infra.mapper.AdminEntityMapper;

import lombok.RequiredArgsConstructor;

/** 관리자 읽기 어댑터 */
@Repository
@RequiredArgsConstructor
public class AdminQueryRepositoryImpl implements AdminQueryRepository {

    private final AdminJpaRepository adminJpaRepository;
    private final AdminEntityMapper adminEntityMapper;

    @Override
    public Optional<Admin> findByEmail(String email) {
        return adminJpaRepository.findByEmail(email).map(adminEntityMapper::toDomain);
    }
}
