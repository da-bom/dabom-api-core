package com.project.family.application.repository;

import java.util.Optional;

import com.project.family.core.Family;

/** 쓰기 전용 포트 */
public interface FamilyCommandRepository {
    Family save(Family family);

    void delete(Long id);

    Optional<Family> findById(Long id);
}
