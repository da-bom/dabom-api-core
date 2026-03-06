package com.project.domain.mission.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.mission.dto.request.CreateMissionRequest;
import com.project.domain.mission.entity.MissionItem;
import com.project.domain.mission.entity.MissionLog;
import com.project.domain.mission.entity.MissionRequest;
import com.project.domain.mission.entity.RewardTemplate;
import com.project.domain.mission.enums.MissionLogActionType;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.enums.RewardCategory;
import com.project.domain.mission.exception.MissionException;
import com.project.domain.mission.model.AuthContext;
import com.project.domain.mission.model.CreateMissionResult;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.model.MissionLogListResult;
import com.project.domain.mission.model.MissionRequestResult;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.mission.repository.RewardTemplateRepository;
import com.project.global.exception.code.MissionErrorCode;

import lombok.RequiredArgsConstructor;

/** 미션 생성/조회/요청/취소 비즈니스 로직 구현체다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionServiceImpl implements MissionService {

    private static final int DEFAULT_CURSOR_SIZE = 20;
    private static final int MAX_CURSOR_SIZE = 100;

    private final MissionItemRepository missionItemRepository;
    private final MissionRequestRepository missionRequestRepository;
    private final MissionLogRepository missionLogRepository;
    private final RewardTemplateRepository rewardTemplateRepository;
    private final CustomerRepository customerRepository;
    private final FamilyMemberRepository familyMemberRepository;

    /** 역할에 따라 OWNER는 가족 전체, MEMBER는 본인 대상 미션만 조회한다. */
    @Override
    public MissionListResult listMissions(AuthContext auth, String status, String cursor, int size) {
        MissionStatus missionStatus = parseMissionStatus(status);
        int pageSize = normalizeSize(size);
        Long cursorId = parseCursor(cursor);
        List<MissionItem> missions = findMissionItemsByRole(auth, missionStatus, cursorId, pageSize);
        boolean hasNext = missions.size() > pageSize;
        List<MissionItem> page = hasNext ? missions.subList(0, pageSize) : missions;
        String nextCursor = hasNext ? String.valueOf(page.get(page.size() - 1).getId()) : null;

        Map<Long, String> customerNameMap = loadCustomerNameMap(page);
        Map<Long, RewardTemplate> rewardTemplateMap = loadRewardTemplateMap(page);
        return new MissionListResult(
                page.stream()
                        .map(mission -> toMissionCard(mission, customerNameMap, rewardTemplateMap))
                        .toList(),
                nextCursor,
                hasNext);
    }

    /** 미션 요청 이력을 커서 기반으로 조회한다. */
    @Override
    public MissionLogListResult listMissionLogs(AuthContext auth, String cursor, int size) {
        int pageSize = normalizeSize(size);
        Long cursorId = parseCursor(cursor);
        List<MissionRequest> requests =
                auth.isOwner()
                        ? missionRequestRepository.findLogsByFamilyScope(
                                auth.familyId(), cursorId, PageRequest.of(0, pageSize + 1))
                        : missionRequestRepository.findLogsByTargetScope(
                                auth.customerId(), cursorId, PageRequest.of(0, pageSize + 1));

        boolean hasNext = requests.size() > pageSize;
        List<MissionRequest> page = hasNext ? requests.subList(0, pageSize) : requests;
        String nextCursor = hasNext ? String.valueOf(page.get(page.size() - 1).getId()) : null;

        Map<Long, MissionItem> missionMap =
                missionItemRepository.findAllById(
                                page.stream().map(MissionRequest::getMissionItemId).collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(MissionItem::getId, Function.identity()));
        Map<Long, String> customerNameMap = loadCustomerNameMapFromRequests(page, missionMap);
        Map<Long, RewardTemplate> rewardTemplateMap = loadRewardTemplateMap(missionMap.values().stream().toList());

        List<MissionLogListResult.MissionLogItem> content =
                page.stream()
                        .map(request -> toMissionLogItem(request, missionMap, customerNameMap, rewardTemplateMap))
                        .toList();

        return new MissionLogListResult(content, nextCursor, hasNext);
    }

    /** OWNER가 같은 가족 MEMBER에게 미션을 생성한다. */
    @Override
    @Transactional
    public CreateMissionResult createMission(AuthContext auth, CreateMissionRequest req) {
        assertOwner(auth);
        FamilyMember targetMember =
                familyMemberRepository
                        .findByCustomerId(req.targetCustomerId())
                        .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_TARGET_INVALID));
        if (!auth.familyId().equals(targetMember.getFamilyId()) || !RoleType.MEMBER.equals(targetMember.getRole())) {
            throw new MissionException(MissionErrorCode.MISSION_TARGET_INVALID);
        }

        RewardTemplate rewardTemplate =
                rewardTemplateRepository
                        .findById(req.rewardTemplateId())
                        .orElseThrow(
                                () ->
                                        new MissionException(
                                                MissionErrorCode.MISSION_REWARD_TEMPLATE_NOT_FOUND));
        RewardCategory requestCategory = parseRewardCategory(req.rewardCategory());
        if (!rewardTemplate.getCategory().equals(requestCategory)) {
            throw new MissionException(MissionErrorCode.MISSION_REWARD_CATEGORY_MISMATCH);
        }

        MissionItem mission =
                missionItemRepository.save(
                        MissionItem.builder()
                                .familyId(auth.familyId())
                                .targetCustomerId(req.targetCustomerId())
                                .createdById(auth.customerId())
                                .rewardTemplateId(req.rewardTemplateId())
                                .missionText(req.missionText())
                                .rewardValue(req.rewardValue())
                                .status(MissionStatus.ACTIVE)
                                .build());

        appendLog(mission.getId(), auth.customerId(), MissionLogActionType.CREATED, "Mission created");
        return new CreateMissionResult(mission.getId(), mission.getCreatedAt());
    }

    /** OWNER가 ACTIVE 미션을 취소한다. */
    @Override
    @Transactional
    public void cancelMission(AuthContext auth, Long missionId) {
        assertOwner(auth);
        MissionItem mission = findMissionByFamilyScope(auth, missionId);
        if (!mission.canCancel()) {
            throw new MissionException(MissionErrorCode.MISSION_INVALID_STATUS_TRANSITION);
        }
        mission.cancel();
        appendLog(mission.getId(), auth.customerId(), MissionLogActionType.CANCELLED, "Mission cancelled");
    }

    /** MEMBER가 본인 미션 완료 요청을 생성한다. */
    @Override
    @Transactional
    public MissionRequestResult requestMissionApproval(AuthContext auth, Long missionId) {
        MissionItem mission = findMissionByFamilyScope(auth, missionId);
        if (!mission.isAssignedTo(auth.customerId())) {
            throw new MissionException(MissionErrorCode.MISSION_NOT_ASSIGNED);
        }
        if (!mission.canRequestReward()) {
            throw new MissionException(MissionErrorCode.MISSION_INVALID_STATUS_TRANSITION);
        }
        if (missionRequestRepository.existsByMissionItemIdAndRequesterIdAndStatus(
                mission.getId(), auth.customerId(), MissionRequestStatus.PENDING)) {
            throw new MissionException(MissionErrorCode.MISSION_REQUEST_DUPLICATED);
        }

        MissionRequest request =
                missionRequestRepository.save(
                        MissionRequest.builder()
                                .missionItemId(mission.getId())
                                .requesterId(auth.customerId())
                                .status(MissionRequestStatus.PENDING)
                                .build());

        appendLog(mission.getId(), auth.customerId(), MissionLogActionType.REQUESTED, "Mission requested");

        RewardTemplate rewardTemplate =
                rewardTemplateRepository
                        .findById(mission.getRewardTemplateId())
                        .orElseThrow(
                                () ->
                                        new MissionException(
                                                MissionErrorCode.MISSION_REWARD_TEMPLATE_NOT_FOUND));
        String requesterName =
                customerRepository.findById(auth.customerId()).map(Customer::getName).orElse("unknown");
        return new MissionRequestResult(
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
                request.getStatus().name(),
                new MissionListResult.CustomerSummary(auth.customerId(), requesterName),
                request.getCreatedAt());
    }

    private List<MissionItem> findMissionItemsByRole(
            AuthContext auth, MissionStatus status, Long cursorId, int pageSize) {
        if (auth.isOwner()) {
            return missionItemRepository.findByFamilyScope(
                    auth.familyId(), status, cursorId, PageRequest.of(0, pageSize + 1));
        }
        return missionItemRepository.findByTargetScope(
                auth.customerId(), status, cursorId, PageRequest.of(0, pageSize + 1));
    }

    private MissionListResult.MissionCard toMissionCard(
            MissionItem mission, Map<Long, String> customerNameMap, Map<Long, RewardTemplate> rewardTemplateMap) {
        RewardTemplate template = rewardTemplateMap.get(mission.getRewardTemplateId());
        if (template == null) {
            throw new MissionException(MissionErrorCode.MISSION_REWARD_TEMPLATE_NOT_FOUND);
        }
        return new MissionListResult.MissionCard(
                mission.getId(),
                mission.getMissionText(),
                mission.getStatus().name(),
                new MissionListResult.CustomerSummary(
                        mission.getTargetCustomerId(),
                        customerNameMap.getOrDefault(mission.getTargetCustomerId(), "unknown")),
                new MissionListResult.CustomerSummary(
                        mission.getCreatedById(),
                        customerNameMap.getOrDefault(mission.getCreatedById(), "unknown")),
                new MissionListResult.RewardTemplate(
                        template.getId(), template.getName(), template.getCategory(), template.getUnit()),
                mission.getRewardValue(),
                mission.getCreatedAt());
    }

    private MissionLogListResult.MissionLogItem toMissionLogItem(
            MissionRequest request,
            Map<Long, MissionItem> missionMap,
            Map<Long, String> customerNameMap,
            Map<Long, RewardTemplate> rewardTemplateMap) {
        MissionItem mission = missionMap.get(request.getMissionItemId());
        if (mission == null) {
            throw new MissionException(MissionErrorCode.MISSION_NOT_FOUND);
        }
        RewardTemplate rewardTemplate = rewardTemplateMap.get(mission.getRewardTemplateId());
        if (rewardTemplate == null) {
            throw new MissionException(MissionErrorCode.MISSION_REWARD_TEMPLATE_NOT_FOUND);
        }
        Long respondedBy = request.getResolvedById();
        return new MissionLogListResult.MissionLogItem(
                request.getId(),
                request.getStatus().name(),
                new MissionLogListResult.MissionItemSimple(
                        mission.getId(),
                        mission.getMissionText(),
                        mission.getRewardValue(),
                        new MissionListResult.RewardTemplate(
                                rewardTemplate.getId(),
                                rewardTemplate.getName(),
                                rewardTemplate.getCategory(),
                                rewardTemplate.getUnit())),
                new MissionListResult.CustomerSummary(
                        mission.getTargetCustomerId(),
                        customerNameMap.getOrDefault(mission.getTargetCustomerId(), "unknown")),
                new MissionListResult.CustomerSummary(
                        request.getRequesterId(),
                        customerNameMap.getOrDefault(request.getRequesterId(), "unknown")),
                respondedBy == null
                        ? null
                        : new MissionListResult.CustomerSummary(
                                respondedBy, customerNameMap.getOrDefault(respondedBy, "unknown")),
                request.getRejectReason(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    private Map<Long, String> loadCustomerNameMap(List<MissionItem> missions) {
        Set<Long> customerIds =
                missions.stream()
                        .flatMap(m -> java.util.stream.Stream.of(m.getTargetCustomerId(), m.getCreatedById()))
                        .collect(Collectors.toSet());
        return customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
    }

    private Map<Long, String> loadCustomerNameMapFromRequests(
            List<MissionRequest> requests, Map<Long, MissionItem> missionMap) {
        Set<Long> customerIds =
                requests.stream()
                        .flatMap(
                                req ->
                                        java.util.stream.Stream.of(
                                                req.getRequesterId(),
                                                req.getResolvedById(),
                                                missionMap.containsKey(req.getMissionItemId())
                                                        ? missionMap.get(req.getMissionItemId()).getTargetCustomerId()
                                                        : null))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());
        return customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
    }

    private Map<Long, RewardTemplate> loadRewardTemplateMap(List<MissionItem> missions) {
        Set<Long> templateIds = missions.stream().map(MissionItem::getRewardTemplateId).collect(Collectors.toSet());
        return rewardTemplateRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(RewardTemplate::getId, Function.identity()));
    }

    private MissionItem findMissionByFamilyScope(AuthContext auth, Long missionId) {
        return missionItemRepository
                .findByIdAndFamilyId(missionId, auth.familyId())
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));
    }

    private void appendLog(
            Long missionItemId, Long actorCustomerId, MissionLogActionType actionType, String message) {
        missionLogRepository.save(
                MissionLog.builder()
                        .missionItemId(missionItemId)
                        .actorId(actorCustomerId)
                        .actionType(actionType)
                        .message(message)
                        .build());
    }

    private void assertOwner(AuthContext auth) {
        if (!auth.isOwner()) {
            throw new MissionException(MissionErrorCode.MISSION_OWNER_ONLY);
        }
    }

    private MissionStatus parseMissionStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MissionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MissionException(MissionErrorCode.MISSION_INVALID_STATUS_TRANSITION);
        }
    }

    private RewardCategory parseRewardCategory(String rewardCategory) {
        try {
            return RewardCategory.valueOf(rewardCategory.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MissionException(MissionErrorCode.MISSION_REWARD_CATEGORY_MISMATCH);
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
            throw new MissionException(MissionErrorCode.MISSION_INVALID_CURSOR);
        }
    }
}
