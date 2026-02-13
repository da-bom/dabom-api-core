package com.project.family.infra.cache;

import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.project.family.core.Family;
import com.project.family.infra.cache.dto.FamilyCacheDto;
import com.project.global.redis.key.RedisKeyGenerator;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FamilyCacheRepository {

    private final RedisTemplate<String, String> familyStringRedisTemplate;

    private final RedisTemplate<String, FamilyCacheDto> familyCacheRedisTemplate;

    private final RedisKeyGenerator redisKeyGenerator;

    public void save(Family family) {
        String key = redisKeyGenerator.generateFamilyInfoKey(family.getId());
        familyCacheRedisTemplate.opsForValue().set(key, FamilyCacheDto.from(family));
    }

    public Optional<Family> findById(Long familyId) {
        String key = redisKeyGenerator.generateFamilyInfoKey(familyId);
        FamilyCacheDto dto = familyCacheRedisTemplate.opsForValue().get(key);

        if (dto == null) {
            return Optional.empty();
        }

        return Optional.of(dto.toDomain());
    }

    public void evict(Long familyId) {
        String key = redisKeyGenerator.generateFamilyInfoKey(familyId);
        familyCacheRedisTemplate.delete(key);
    }

    public Optional<Long> findFamilyRemainingBytes(Long familyId) {
        String key = redisKeyGenerator.generateFamilyRemainingKey(familyId);
        return findLongValue(key);
    }

    public Optional<Long> findUserMonthlyUsageBytes(Long familyId, Long userId) {
        String key = redisKeyGenerator.generateFamilyUserMonthlyUsageKey(familyId, userId);
        return findLongValue(key);
    }

    private Optional<Long> findLongValue(String key) {
        String raw = familyStringRedisTemplate.opsForValue().get(key);

        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
