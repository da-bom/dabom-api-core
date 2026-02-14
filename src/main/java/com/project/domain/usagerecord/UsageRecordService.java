package com.project.domain.usagerecord;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.usagerecord.repository.UsageSseEmitterRegistry;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageRecordService {

    private final UsageSseEmitterRegistry registry;
    private final FamilyCacheRepository familyCacheRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public SseEmitter subscribe(Long customerId) {
        Long familyId =
                familyMemberRepository
                        .findFamilyIdByCustomerId(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        return registry.register(familyId);
    }

    public void pushLatest(Long familyId) {
        Optional<Long> latest = familyCacheRepository.findFamilyRemainingBytes(familyId);
        if (latest.isEmpty()) {
            log.info("family id {} is not found in cache", familyId);
            return; // TODO: 데이터베이스를 조회하게 수정
        }

        registry.send(familyId, "usage-updated", latest);
    }
}
