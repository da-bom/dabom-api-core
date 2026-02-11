package com.project.family.infra.repository;

import java.util.Optional;

import com.project.family.infra.entity.FamilyJpaEntity;
import org.springframework.stereotype.Repository;

import com.project.family.application.repository.FamilyCommandRepository;
import com.project.family.core.Family;
import com.project.family.infra.mapper.FamilyEntityMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FamilyCommandRepositoryImpl implements FamilyCommandRepository {

    private final JpaFamilyRepository jpaFamilyRepository;

    @Override
    public Family save(Family family) {
        FamilyJpaEntity entity = FamilyEntityMapper.toEntity(family);
        FamilyJpaEntity saved = jpaFamilyRepository.save(entity);
        return FamilyEntityMapper.toDomain(saved);
    }

    @Override
    public void delete(Long id) {
        jpaFamilyRepository.deleteById(id);
    }

    @Override
    public Optional<Family> findById(Long id) {
        return jpaFamilyRepository.findById(id).map(FamilyEntityMapper::toDomain);
    }
}
