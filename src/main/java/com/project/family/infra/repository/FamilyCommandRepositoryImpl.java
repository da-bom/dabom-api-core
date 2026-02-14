package com.project.family.infra.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.project.family.application.repository.FamilyCommandRepository;
import com.project.family.core.Family;
import com.project.family.infra.cache.FamilyCacheRepository;
import com.project.family.infra.entity.FamilyJpaEntity;
import com.project.family.infra.mapper.FamilyEntityMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FamilyCommandRepositoryImpl implements FamilyCommandRepository {

    private final JpaFamilyRepository jpaFamilyRepository;
    private final FamilyCacheRepository familyCacheRepository;

    @Override
    public Family save(Family family) {
        FamilyJpaEntity entity = FamilyEntityMapper.toEntity(family);
        FamilyJpaEntity saved = jpaFamilyRepository.save(entity);
        familyCacheRepository.evict(saved.getId());
        return FamilyEntityMapper.toDomain(saved);
    }

    @Override
    public void delete(Long id) {
        jpaFamilyRepository.deleteById(id);
        familyCacheRepository.evict(id);
    }

    @Override
    public Optional<Family> findById(Long id) {
        Optional<Family> cached = familyCacheRepository.findById(id);
        if (cached.isPresent()) {
            return cached;
        }

        return jpaFamilyRepository
                .findById(id)
                .map(FamilyEntityMapper::toDomain)
                .map(
                        family -> {
                            familyCacheRepository.save(family);
                            return family;
                        });
    }
}
