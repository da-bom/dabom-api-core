package com.project.domain.reward.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.mission.entity.MissionLog;
import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.enums.MissionLogActionType;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.model.MissionLogListResult;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.reward.dto.request.RespondRewardRequest;
import com.project.domain.reward.entity.RewardGrant;
import com.project.domain.reward.enums.RewardGrantStatus;
import com.project.domain.reward.model.ReceivedRewardListResult;
import com.project.domain.reward.model.RewardRespondResult;
import com.project.domain.reward.repository.RewardGrantRepository;
import com.project.domain.reward.support.RewardDtoMapper;
import com.project.global.auth.model.AuthContext;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.MissionErrorCode;

import lombok.RequiredArgsConstructor;

/** 보상 요청 승인/거절과 수령 내역 조회를 처리하는 서비스 구현체. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardServiceImpl implements RewardService {

    private static final int DEFAULT_CURSOR_SIZE = 20;
    private static final int MAX_CURSOR_SIZE = 100;
    private static final String UNKNOWN_NAME = "unknown";

    private final Clock clock;
    private final MissionRequestRepository missionRequestRepository;
    private final MissionItemRepository missionItemRepository;
    private final MissionLogRepository missionLogRepository;
    private final CustomerRepository customerRepository;
    private final RewardGrantRepository rewardGrantRepository;

    /** OWNER가 보상 요청을 승인하거나 거절한다. */
    @Override
    @Transactional
    public RewardRespondResult respondRewardRequest(
            AuthContext auth, Long requestId, RespondRewardRequest req) {
        // 1. 요청자가 OWNER인지, 요청이 아직 처리 가능한 상태인지 확인한다.
        assertOwner(auth);

        MissionRequest missionRequest =
                missionRequestRepository
                        .findByIdForUpdate(requestId)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                MissionErrorCode.MISSION_REQUEST_NOT_FOUND));
        if (!missionRequest.isPending()) {
            throw new ApplicationException(MissionErrorCode.MISSION_REQUEST_INVALID_STATUS);
        }

        MissionRequestStatus requestedStatus = parseRequestedStatus(req.status());
        MissionItem mission =
                missionItemRepository
                        .findByIdAndFamilyIdForUpdate(
                                missionRequest.getMissionItemId(), auth.familyId())
                        .orElseThrow(
                                () -> new ApplicationException(MissionErrorCode.MISSION_NOT_FOUND));

        // 2. 승인 시 미션을 완료 처리하고, 거절 시 사유를 필수로 받는다.
        if (MissionRequestStatus.APPROVED.equals(requestedStatus)) {
            if (!mission.canComplete()) {
                throw new ApplicationException(MissionErrorCode.MISSION_INVALID_STATUS_TRANSITION);
            }
            LocalDateTime now = LocalDateTime.now(clock);
            missionRequest.approve(auth.customerId(), now);
            mission.complete(now);
            appendLog(
                    mission.getId(),
                    auth.customerId(),
                    MissionLogActionType.COMPLETED,
                    "Mission completed");

            Customer requester =
                    customerRepository
                            .findById(missionRequest.getRequesterId())
                            .orElseThrow(
                                    () ->
                                            new ApplicationException(
                                                    MissionErrorCode.MISSION_TARGET_INVALID));
            rewardGrantRepository.save(
                    RewardGrant.builder()
                            .reward(mission.getReward())
                            .customer(requester)
                            .missionItem(mission)
                            .status(RewardGrantStatus.ISSUED)
                            .build());
        } else {
            if (req.rejectReason() == null || req.rejectReason().isBlank()) {
                throw new ApplicationException(MissionErrorCode.MISSION_REJECT_REASON_REQUIRED);
            }
            missionRequest.reject(auth.customerId(), req.rejectReason(), LocalDateTime.now(clock));
        }

        // 3. 응답에는 MissionItem에 연결된 Reward 스냅샷을 포함한다.
        String responderName =
                customerRepository
                        .findById(auth.customerId())
                        .map(Customer::getName)
                        .orElse(UNKNOWN_NAME);

        return new RewardRespondResult(
                missionRequest.getId(),
                missionRequest.getStatus().name(),
                new RewardRespondResult.MissionItemWithStatus(
                        mission.getId(),
                        mission.getMissionText(),
                        mission.getStatus().name(),
                        RewardDtoMapper.toModel(mission.getReward())),
                new MissionListResult.CustomerSummary(auth.customerId(), responderName),
                missionRequest.getRejectReason(),
                missionRequest.getUpdatedAt());
    }

    /** 본인이 승인받은 보상 수령 내역을 커서 기반으로 조회한다. */
    @Override
    public ReceivedRewardListResult listReceivedRewards(AuthContext auth, String cursor, int size) {
        // 1. 커서 입력을 정규화한다.
        int pageSize = normalizeSize(size);
        Long cursorId = parseCursor(cursor);

        // 2. 승인된 요청 목록과 연결된 미션, 승인자 정보를 함께 조회한다.
        List<MissionRequest> requests =
                missionRequestRepository.findByRequesterIdAndStatusOrderByIdDesc(
                        auth.customerId(),
                        MissionRequestStatus.APPROVED,
                        cursorId,
                        PageRequest.of(0, pageSize + 1));

        boolean hasNext = requests.size() > pageSize;
        List<MissionRequest> page = hasNext ? requests.subList(0, pageSize) : requests;
        String nextCursor = hasNext ? String.valueOf(page.getLast().getId()) : null;

        Set<Long> missionIds =
                page.stream().map(MissionRequest::getMissionItemId).collect(Collectors.toSet());
        Map<Long, MissionItem> missionMap =
                missionItemRepository.findAllWithRewardByIdIn(missionIds).stream()
                        .collect(Collectors.toMap(MissionItem::getId, Function.identity()));

        Set<Long> approverIds =
                page.stream().map(MissionRequest::getResolvedById).collect(Collectors.toSet());
        Map<Long, String> approverNameMap =
                customerRepository.findAllById(approverIds).stream()
                        .collect(Collectors.toMap(Customer::getId, Customer::getName));

        List<ReceivedRewardListResult.ReceivedRewardItem> content =
                page.stream()
                        .map(req -> toReceivedRewardItem(req, missionMap, approverNameMap))
                        .toList();

        return new ReceivedRewardListResult(content, nextCursor, hasNext);
    }

    /** 승인된 보상 요청 엔티티를 수령 내역 응답 모델로 변환한다. */
    private ReceivedRewardListResult.ReceivedRewardItem toReceivedRewardItem(
            MissionRequest request,
            Map<Long, MissionItem> missionMap,
            Map<Long, String> approverNameMap) {
        MissionItem mission = missionMap.get(request.getMissionItemId());
        if (mission == null) {
            throw new ApplicationException(MissionErrorCode.MISSION_NOT_FOUND);
        }
        Long approverId = request.getResolvedById();
        return new ReceivedRewardListResult.ReceivedRewardItem(
                request.getId(),
                new MissionLogListResult.MissionItemSimple(
                        mission.getId(),
                        mission.getMissionText(),
                        RewardDtoMapper.toModel(mission.getReward())),
                approverId == null
                        ? null
                        : new MissionListResult.CustomerSummary(
                                approverId, approverNameMap.getOrDefault(approverId, UNKNOWN_NAME)),
                request.getResolvedAt());
    }

    /** 보상 요청 처리 이력을 로그로 남긴다. */
    private void appendLog(
            Long missionItemId,
            Long actorCustomerId,
            MissionLogActionType actionType,
            String message) {
        missionLogRepository.save(
                MissionLog.builder()
                        .missionItemId(missionItemId)
                        .actorId(actorCustomerId)
                        .actionType(actionType)
                        .message(message)
                        .build());
    }

    /** 응답 요청 status 문자열을 허용된 상태값으로 파싱한다. */
    private MissionRequestStatus parseRequestedStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ApplicationException(MissionErrorCode.MISSION_INVALID_REQUEST_STATUS);
        }
        try {
            MissionRequestStatus parsed = MissionRequestStatus.valueOf(status.toUpperCase());
            if (MissionRequestStatus.PENDING.equals(parsed)) {
                throw new ApplicationException(MissionErrorCode.MISSION_INVALID_REQUEST_STATUS);
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(MissionErrorCode.MISSION_INVALID_REQUEST_STATUS);
        }
    }

    /** OWNER 전용 기능인지 검증한다. */
    private void assertOwner(AuthContext auth) {
        if (!auth.isOwner()) {
            throw new ApplicationException(MissionErrorCode.MISSION_OWNER_ONLY);
        }
    }

    /** 요청한 페이지 크기를 허용 범위로 보정한다. */
    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_CURSOR_SIZE;
        }
        return Math.min(size, MAX_CURSOR_SIZE);
    }

    /** 커서 문자열을 Long으로 파싱한다. */
    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(cursor);
        } catch (NumberFormatException e) {
            throw new ApplicationException(MissionErrorCode.MISSION_INVALID_CURSOR);
        }
    }
}
