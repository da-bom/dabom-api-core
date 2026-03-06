package com.project.domain.mission.service;

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
import com.project.domain.mission.dto.request.RespondRewardRequest;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.mission.entity.MissionLog;
import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.entity.RewardTemplate;
import com.project.domain.mission.enums.MissionLogActionType;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.model.AuthContext;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.model.MissionLogListResult;
import com.project.domain.mission.model.ReceivedRewardListResult;
import com.project.domain.mission.model.RewardRespondResult;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.mission.repository.RewardTemplateRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.MissionErrorCode;

import lombok.RequiredArgsConstructor;

/** 보상 요청 승인/거절 및 수령내역 조회 구현체다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardServiceImpl implements RewardService {

    private static final int DEFAULT_CURSOR_SIZE = 20;
    private static final int MAX_CURSOR_SIZE = 100;

    private final MissionRequestRepository missionRequestRepository;
    private final MissionItemRepository missionItemRepository;
    private final MissionLogRepository missionLogRepository;
    private final RewardTemplateRepository rewardTemplateRepository;
    private final CustomerRepository customerRepository;

    /** OWNER가 보상 요청을 승인/거절 처리한다. */
    @Override
    @Transactional
    public RewardRespondResult respondRewardRequest(
            AuthContext auth, Long requestId, RespondRewardRequest req) {
        assertOwner(auth);

        MissionRequest missionRequest =
                missionRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                MissionErrorCode.MISSION_REQUEST_NOT_FOUND));
        if (!missionRequest.isPending()) {
            throw new ApplicationException(MissionErrorCode.MISSION_REQUEST_INVALID_STATUS);
        }

        MissionRequestStatus requestedStatus = parseRequestedStatus(req.status());
        MissionItem mission = findMissionByFamilyScope(auth, missionRequest.getMissionItemId());

        if (MissionRequestStatus.APPROVED.equals(requestedStatus)) {
            if (!mission.canComplete()) {
                throw new ApplicationException(MissionErrorCode.MISSION_INVALID_STATUS_TRANSITION);
            }
            missionRequest.approve(auth.customerId(), LocalDateTime.now());
            mission.complete(LocalDateTime.now());
            appendLog(
                    mission.getId(),
                    auth.customerId(),
                    MissionLogActionType.APPROVED,
                    "Reward approved");
        } else {
            if (req.rejectReason() == null || req.rejectReason().isBlank()) {
                throw new ApplicationException(MissionErrorCode.MISSION_REJECT_REASON_REQUIRED);
            }
            missionRequest.reject(auth.customerId(), req.rejectReason(), LocalDateTime.now());
            appendLog(
                    mission.getId(),
                    auth.customerId(),
                    MissionLogActionType.REJECTED,
                    "Reward rejected");
        }

        RewardTemplate rewardTemplate =
                rewardTemplateRepository
                        .findById(mission.getRewardTemplateId())
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                MissionErrorCode
                                                        .MISSION_REWARD_TEMPLATE_NOT_FOUND));
        String responderName =
                customerRepository
                        .findById(auth.customerId())
                        .map(Customer::getName)
                        .orElse("unknown");

        return new RewardRespondResult(
                missionRequest.getId(),
                missionRequest.getStatus().name(),
                new RewardRespondResult.MissionItemWithStatus(
                        mission.getId(),
                        mission.getMissionText(),
                        mission.getStatus().name(),
                        mission.getRewardValue(),
                        new MissionListResult.RewardTemplate(
                                rewardTemplate.getId(),
                                rewardTemplate.getName(),
                                rewardTemplate.getCategory(),
                                rewardTemplate.getUnit())),
                new MissionListResult.CustomerSummary(auth.customerId(), responderName),
                missionRequest.getRejectReason(),
                missionRequest.getUpdatedAt());
    }

    /** MEMBER 본인이 승인받은 보상 수령내역을 조회한다. */
    @Override
    public ReceivedRewardListResult listReceivedRewards(AuthContext auth, String cursor, int size) {
        int pageSize = normalizeSize(size);
        Long cursorId = parseCursor(cursor);
        List<MissionRequest> requests =
                missionRequestRepository.findApprovedByTargetCustomerIdOrderByResolvedAtDesc(
                        auth.customerId(), cursorId, PageRequest.of(0, pageSize + 1));

        boolean hasNext = requests.size() > pageSize;
        List<MissionRequest> page = hasNext ? requests.subList(0, pageSize) : requests;
        String nextCursor = hasNext ? String.valueOf(page.get(page.size() - 1).getId()) : null;

        Set<Long> missionIds =
                page.stream().map(MissionRequest::getMissionItemId).collect(Collectors.toSet());
        Map<Long, MissionItem> missionMap =
                missionItemRepository.findAllById(missionIds).stream()
                        .collect(Collectors.toMap(MissionItem::getId, Function.identity()));
        Set<Long> rewardTemplateIds =
                missionMap.values().stream()
                        .map(MissionItem::getRewardTemplateId)
                        .collect(Collectors.toSet());
        Map<Long, RewardTemplate> rewardTemplateMap =
                rewardTemplateRepository.findAllById(rewardTemplateIds).stream()
                        .collect(Collectors.toMap(RewardTemplate::getId, Function.identity()));

        Set<Long> approverIds =
                page.stream().map(MissionRequest::getResolvedById).collect(Collectors.toSet());
        Map<Long, String> approverNameMap =
                customerRepository.findAllById(approverIds).stream()
                        .collect(Collectors.toMap(Customer::getId, Customer::getName));

        List<ReceivedRewardListResult.ReceivedRewardItem> content =
                page.stream()
                        .map(
                                req ->
                                        toReceivedRewardItem(
                                                req,
                                                missionMap,
                                                rewardTemplateMap,
                                                approverNameMap))
                        .toList();

        return new ReceivedRewardListResult(content, nextCursor, hasNext);
    }

    private ReceivedRewardListResult.ReceivedRewardItem toReceivedRewardItem(
            MissionRequest request,
            Map<Long, MissionItem> missionMap,
            Map<Long, RewardTemplate> rewardTemplateMap,
            Map<Long, String> approverNameMap) {
        MissionItem mission = missionMap.get(request.getMissionItemId());
        if (mission == null) {
            throw new ApplicationException(MissionErrorCode.MISSION_NOT_FOUND);
        }
        RewardTemplate rewardTemplate = rewardTemplateMap.get(mission.getRewardTemplateId());
        if (rewardTemplate == null) {
            throw new ApplicationException(MissionErrorCode.MISSION_REWARD_TEMPLATE_NOT_FOUND);
        }
        Long approverId = request.getResolvedById();
        return new ReceivedRewardListResult.ReceivedRewardItem(
                request.getId(),
                new MissionLogListResult.MissionItemSimple(
                        mission.getId(),
                        mission.getMissionText(),
                        mission.getRewardValue(),
                        new MissionListResult.RewardTemplate(
                                rewardTemplate.getId(),
                                rewardTemplate.getName(),
                                rewardTemplate.getCategory(),
                                rewardTemplate.getUnit())),
                approverId == null
                        ? null
                        : new MissionListResult.CustomerSummary(
                                approverId, approverNameMap.getOrDefault(approverId, "unknown")),
                request.getResolvedAt());
    }

    private MissionItem findMissionByFamilyScope(AuthContext auth, Long missionId) {
        return missionItemRepository
                .findByIdAndFamilyId(missionId, auth.familyId())
                .orElseThrow(() -> new ApplicationException(MissionErrorCode.MISSION_NOT_FOUND));
    }

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

    private void assertOwner(AuthContext auth) {
        if (!auth.isOwner()) {
            throw new ApplicationException(MissionErrorCode.MISSION_OWNER_ONLY);
        }
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_CURSOR_SIZE;
        }
        return Math.min(size, MAX_CURSOR_SIZE);
    }

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
