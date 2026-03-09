package com.project.domain.appeal.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.appeal.entity.PolicyAppeal;
import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.model.AppealListResult;
import com.project.domain.appeal.repository.PolicyAppealRepository;
import com.project.global.auth.model.AuthContext;

import lombok.RequiredArgsConstructor;

/** 이의제기 목록 조회 서비스 구현 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppealServiceImpl implements AppealService {

    private static final int DEFAULT_CURSOR_SIZE = 20;
    private static final int MAX_CURSOR_SIZE = 100;

    private final PolicyAppealRepository policyAppealRepository;

    /** 이의제기 목록 조회 */
    @Override
    public AppealListResult getAppeals(
            AuthContext auth, AppealStatus status, Long cursor, int size) {
        // 1. 요청 size를 기본값과 최대값 범위 안으로 보정한다.
        int pageSize = normalizeSize(size);
        // 2. 역할에 따라 OWNER는 가족 전체, MEMBER는 본인 이의제기만 조회한다.
        List<PolicyAppeal> appeals =
                auth.isOwner()
                        ? policyAppealRepository.findAllByFamilyId(
                                auth.familyId(), status, cursor, PageRequest.of(0, pageSize + 1))
                        : policyAppealRepository.findByRequesterIdAndFamilyId(
                                auth.customerId(),
                                auth.familyId(),
                                status,
                                cursor,
                                PageRequest.of(0, pageSize + 1));

        // 3. pageSize + 1 조회 결과로 다음 페이지 존재 여부와 다음 커서를 계산한다.
        boolean hasNext = appeals.size() > pageSize;
        List<PolicyAppeal> page = hasNext ? appeals.subList(0, pageSize) : appeals;
        String nextCursor = hasNext ? String.valueOf(page.getLast().getId()) : null;

        // 4. 현재 페이지 엔티티를 응답 모델로 변환해 목록 결과를 반환한다.
        return new AppealListResult(
                page.stream().map(this::toAppealSummary).toList(), nextCursor, hasNext);
    }

    /** 이의제기 요약 모델 변환 */
    private AppealListResult.AppealSummary toAppealSummary(PolicyAppeal appeal) {
        return new AppealListResult.AppealSummary(
                appeal.getId(),
                appeal.getType(),
                appeal.getPolicyAssignment() == null ? null : appeal.getPolicyAssignment().getId(),
                appeal.getRequester().getId(),
                appeal.getRequester().getName(),
                appeal.getRequestReason(),
                appeal.getDesiredRules(),
                appeal.getStatus(),
                appeal.getCreatedAt());
    }

    /** 커서 페이지 크기 보정 */
    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_CURSOR_SIZE;
        }
        return Math.min(size, MAX_CURSOR_SIZE);
    }
}
