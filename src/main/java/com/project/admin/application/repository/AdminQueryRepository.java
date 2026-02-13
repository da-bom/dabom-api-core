package com.project.admin.application.repository;

import java.util.Optional;

import com.project.admin.core.Admin;

/** 관리자 조회 포트 */
public interface AdminQueryRepository {

    Optional<Admin> findByEmail(String email);
}
