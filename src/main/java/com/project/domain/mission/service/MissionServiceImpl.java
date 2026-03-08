package com.project.domain.mission.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.dao.DataIntegrityViolationException;
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
import com.project.domain.mission.model.AuthContext;
import com.project.domain.mission.model.CreateMissionResult;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.model.MissionLogListResult;
import com.project.domain.mission.model.MissionRequestResult;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.mission.repository.RewardTemplateRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.MissionErrorCode;

import lombok.RequiredArgsConstructor;

/** 미션 애플리케이션 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionServiceImpl implements MissionService {

    private static final int DEFAULT_CURSOR_SIZE = 20;
    private static final int MAX_CURSOR_SIZE = 100;
    private static final String UNKNOWN_NAME = "unknown";

    private final MissionItemRepository missionItemRepository;
    private final MissionRequestRepository missionRequestRepository;
    private final MissionLogRepository missionLogRepository;
    private final RewardTemplateRepository rewardTemplateRepository;
    private final CustomerRepository customerRepository;
    private final FamilyMemberRepository familyMemberRepository;

    /** 미션 목록 조회. */
    @Override
    public MissionListResult listMissions(AuthContext auth, String cursor, int size) {
        // 1. 커서 페이징 입력값을 정규화한다.
        int pageSize = normalizeSize(size);
        Long cursorId = parseCursor(cursor);

        // 2. 호출자 역할에 맞는 ACTIVE 미션 목록만 조회한다.
        List<MissionItem> missions = findActiveMissionItemsByRole(auth, cursorId, pageSize);
        boolean hasNext = missions.size() > pageSize;
        List<MissionItem> page = hasNext ? missions.subList(0, pageSize) : missions;
        String nextCursor = hasNext ? String.valueOf(page.getLast().getId()) : null;

        // 3. 응답 조합에 필요한 연관 데이터와 요청 상태를 조회한다.
        Map<Long, String> customerNameMap = loadCustomerNameMap(page);
        Map<Long, RewardTemplate> rewardTemplateMap = loadRewardTemplateMap(page);
        Map<Long, String> requestStatusMap = loadMissionRequestStatusMap(page);

        // 4. 조회한 도메인 데이터를 응답 모델로 변환한다.
        return new MissionListResult(
                page.stream()
                        .map(
                                mission ->
                                        toMissionCard(
                                                mission,
                                                customerNameMap,
                                                rewardTemplateMap,
                                                requestStatusMap))
                        .toList(),
                nextCursor,
                hasNext);
    }

    /** 미션 로그 조회. */
    @Override
    public MissionLogListResult listMissionLogs(AuthContext auth, String cursor, int size) {
        // 1. 커서 페이징 입력값을 정규화한다.
        int pageSize = normalizeSize(size);
        Long cursorId = parseCursor(cursor);

        // 2. 호출자 권한 범위에 맞는 미션 로그를 조회한다.
        List<MissionLog> logs =
                auth.isOwner()
                        ? missionLogRepository.findByFamilyScope(
                                auth.familyId(), cursorId, PageRequest.of(0, pageSize + 1))
                        : missionLogRepository.findByTargetScope(
                                auth.customerId(), cursorId, PageRequest.of(0, pageSize + 1));

        boolean hasNext = logs.size() > pageSize;
        List<MissionLog> page = hasNext ? logs.subList(0, pageSize) : logs;
        String nextCursor = hasNext ? String.valueOf(page.getLast().getId()) : null;

        // 3. 응답 조합에 필요한 미션, 사용자, 보상 템플릿 정보를 조회한다.
        Map<Long, MissionItem> missionMap =
                missionItemRepository
                        .findAllById(
                                page.stream()
                                        .map(MissionLog::getMissionItemId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(MissionItem::getId, Function.identity()));
        Map<Long, String> customerNameMap = loadCustomerNameMapFromLogs(page, missionMap);
        Map<Long, RewardTemplate> rewardTemplateMap =
                loadRewardTemplateMap(missionMap.values().stream().toList());

        // 4. 조회한 도메인 데이터를 응답 모델로 변환한다.
        List<MissionLogListResult.MissionLogItem> missionLogItems =
                page.stream()
                        .map(
                                log ->
                                        toMissionLogItem(
                                                log,
                                                missionMap,
                                                customerNameMap,
                                                rewardTemplateMap))
                        .toList();

        return new MissionLogListResult(missionLogItems, nextCursor, hasNext);
    }

    /** 미션 생성. */
    @Override
    @Transactional
    public CreateMissionResult createMission(AuthContext auth, CreateMissionRequest req) {
        // 1. 요청자 권한과 대상 가족 구성원을 검증한다.
        assertOwner(auth);
        FamilyMember targetMember =
                familyMemberRepository
                        .findByCustomerId(req.targetCustomerId())
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                MissionErrorCode.MISSION_TARGET_INVALID));
        if (!auth.familyId().equals(targetMember.getFamilyId())
                || !RoleType.MEMBER.equals(targetMember.getRole())) {
            throw new ApplicationException(MissionErrorCode.MISSION_TARGET_INVALID);
        }

        // 2. 보상 템플릿과 보상 카테고리 일치 여부를 검증한다.
        RewardTemplate rewardTemplate =
                rewardTemplateRepository
                        .findById(req.rewardTemplateId())
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                MissionErrorCode
                                                        .MISSION_REWARD_TEMPLATE_NOT_FOUND));
        RewardCategory requestCategory = parseRewardCategory(req.rewardCategory());
        if (!rewardTemplate.getCategory().equals(requestCategory)) {
            throw new ApplicationException(MissionErrorCode.MISSION_REWARD_CATEGORY_MISMATCH);
        }

        // 3. 미션을 저장하고 생성 로그를 남긴다.
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

        appendLog(
                mission.getId(),
                auth.customerId(),
                MissionLogActionType.CREATED,
                "Mission created");
        return new CreateMissionResult(mission.getId(), mission.getCreatedAt());
    }

    /** 미션 취소. */
    @Override
    @Transactional
    public void cancelMission(AuthContext auth, Long missionId) {
        // 1. 요청자 권한과 현재 미션 상태를 검증한다.
        assertOwner(auth);
        MissionItem mission = findMissionByFamilyScopeForUpdate(auth, missionId);
        if (!mission.canCancel()) {
            throw new ApplicationException(MissionErrorCode.MISSION_INVALID_STATUS_TRANSITION);
        }

        // 2. 미션 상태를 변경하고 취소 로그를 남긴다.
        mission.cancel();
        appendLog(
                mission.getId(),
                auth.customerId(),
                MissionLogActionType.CANCELLED,
                "Mission cancelled");
    }

    /** 미션 승인 요청 생성. */
    @Override
    @Transactional
    public MissionRequestResult requestMissionApproval(AuthContext auth, Long missionId) {
        // 1. 미션 담당자, 미션 상태, 중복 대기 요청 여부를 검증한다.
        MissionItem mission = findMissionByFamilyScopeForUpdate(auth, missionId);
        if (!mission.isAssignedTo(auth.customerId())) {
            throw new ApplicationException(MissionErrorCode.MISSION_NOT_ASSIGNED);
        }
        if (!mission.canRequestReward()) {
            throw new ApplicationException(MissionErrorCode.MISSION_INVALID_STATUS_TRANSITION);
        }
        if (missionRequestRepository.existsByMissionItemIdAndRequesterIdAndStatus(
                mission.getId(), auth.customerId(), MissionRequestStatus.PENDING)) {
            throw new ApplicationException(MissionErrorCode.MISSION_REQUEST_DUPLICATED);
        }

        // 2. 승인 요청을 생성하고 요청 로그를 남긴다.
        MissionRequest request;
        try {
            request =
                    missionRequestRepository.save(
                            MissionRequest.builder()
                                    .missionItemId(mission.getId())
                                    .requesterId(auth.customerId())
                                    .status(MissionRequestStatus.PENDING)
                                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(MissionErrorCode.MISSION_REQUEST_DUPLICATED);
        }

        appendLog(
                mission.getId(),
                auth.customerId(),
                MissionLogActionType.REQUESTED,
                "Mission requested");

        // 3. 응답 조합에 필요한 데이터를 조회한다.
        RewardTemplate rewardTemplate =
                rewardTemplateRepository
                        .findById(mission.getRewardTemplateId())
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                MissionErrorCode
                                                        .MISSION_REWARD_TEMPLATE_NOT_FOUND));
        String requesterName =
                customerRepository
                        .findById(auth.customerId())
                        .map(Customer::getName)
                        .orElse(UNKNOWN_NAME);
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

    /** 역할별 활성 미션 조회. */
    private List<MissionItem> findActiveMissionItemsByRole(
            AuthContext auth, Long cursorId, int pageSize) {
        if (auth.isOwner()) {
            return missionItemRepository.findByFamilyScope(
                    auth.familyId(),
                    MissionStatus.ACTIVE,
                    cursorId,
                    PageRequest.of(0, pageSize + 1));
        }
        return missionItemRepository.findByTargetScope(
                auth.customerId(), MissionStatus.ACTIVE, cursorId, PageRequest.of(0, pageSize + 1));
    }

    /** 미션 카드 변환. */
    private MissionListResult.MissionCard toMissionCard(
            MissionItem mission,
            Map<Long, String> customerNameMap,
            Map<Long, RewardTemplate> rewardTemplateMap,
            Map<Long, String> requestStatusMap) {
        RewardTemplate template = rewardTemplateMap.get(mission.getRewardTemplateId());
        if (template == null) {
            throw new ApplicationException(MissionErrorCode.MISSION_REWARD_TEMPLATE_NOT_FOUND);
        }
        return new MissionListResult.MissionCard(
                mission.getId(),
                mission.getMissionText(),
                requestStatusMap.get(mission.getId()),
                new MissionListResult.CustomerSummary(
                        mission.getTargetCustomerId(),
                        customerNameMap.getOrDefault(mission.getTargetCustomerId(), UNKNOWN_NAME)),
                new MissionListResult.CustomerSummary(
                        mission.getCreatedById(),
                        customerNameMap.getOrDefault(mission.getCreatedById(), UNKNOWN_NAME)),
                new MissionListResult.RewardTemplate(
                        template.getId(),
                        template.getName(),
                        template.getCategory(),
                        template.getUnit()),
                mission.getRewardValue(),
                mission.getCreatedAt());
    }

    /** 미션 요청 상태 맵 조회. */
    private Map<Long, String> loadMissionRequestStatusMap(List<MissionItem> missions) {
        Set<Long> missionIds =
                missions.stream().map(MissionItem::getId).collect(Collectors.toSet());
        return missionRequestRepository
                .findByMissionItemIdInOrderByCreatedAtDescIdDesc(missionIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                MissionRequest::getMissionItemId,
                                request -> request.getStatus().name(),
                                (latest, ignored) -> latest));
    }

    /** 미션 로그 응답 변환. */
    private MissionLogListResult.MissionLogItem toMissionLogItem(
            MissionLog log,
            Map<Long, MissionItem> missionMap,
            Map<Long, String> customerNameMap,
            Map<Long, RewardTemplate> rewardTemplateMap) {
        MissionItem mission = missionMap.get(log.getMissionItemId());
        if (mission == null) {
            throw new ApplicationException(MissionErrorCode.MISSION_NOT_FOUND);
        }
        RewardTemplate rewardTemplate = rewardTemplateMap.get(mission.getRewardTemplateId());
        if (rewardTemplate == null) {
            throw new ApplicationException(MissionErrorCode.MISSION_REWARD_TEMPLATE_NOT_FOUND);
        }
        return new MissionLogListResult.MissionLogItem(
                log.getId(),
                log.getActionType().name(),
                log.getMessage(),
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
                        customerNameMap.getOrDefault(mission.getTargetCustomerId(), UNKNOWN_NAME)),
                log.getActorId() == null
                        ? null
                        : new MissionListResult.CustomerSummary(
                                log.getActorId(),
                                customerNameMap.getOrDefault(log.getActorId(), UNKNOWN_NAME)),
                log.getCreatedAt());
    }

    /** 미션 카드용 사용자 이름 맵 조회. */
    private Map<Long, String> loadCustomerNameMap(List<MissionItem> missions) {
        Set<Long> customerIds =
                missions.stream()
                        .flatMap(
                                mission ->
                                        Stream.of(
                                                mission.getTargetCustomerId(),
                                                mission.getCreatedById()))
                        .collect(Collectors.toSet());
        return customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
    }

    /** 미션 로그용 사용자 이름 맵 조회. */
    private Map<Long, String> loadCustomerNameMapFromLogs(
            List<MissionLog> logs, Map<Long, MissionItem> missionMap) {
        Set<Long> customerIds =
                logs.stream()
                        .flatMap(
                                log ->
                                        Stream.of(
                                                log.getActorId(),
                                                missionMap.containsKey(log.getMissionItemId())
                                                        ? missionMap
                                                                .get(log.getMissionItemId())
                                                                .getTargetCustomerId()
                                                        : null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        return customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
    }

    /** 보상 템플릿 맵 조회. */
    private Map<Long, RewardTemplate> loadRewardTemplateMap(List<MissionItem> missions) {
        Set<Long> templateIds =
                missions.stream().map(MissionItem::getRewardTemplateId).collect(Collectors.toSet());
        return rewardTemplateRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(RewardTemplate::getId, Function.identity()));
    }


    /** 가족 범위 미션 조회 락 획득. */
    private MissionItem findMissionByFamilyScopeForUpdate(AuthContext auth, Long missionId) {
        return missionItemRepository
                .findByIdAndFamilyIdForUpdate(missionId, auth.familyId())
                .orElseThrow(() -> new ApplicationException(MissionErrorCode.MISSION_NOT_FOUND));
    }

    /** 미션 로그 저장. */
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

    /** 소유자 권한 검증. */
    private void assertOwner(AuthContext auth) {
        if (!auth.isOwner()) {
            throw new ApplicationException(MissionErrorCode.MISSION_OWNER_ONLY);
        }
    }

    /** 보상 카테고리 파싱. */
    private RewardCategory parseRewardCategory(String rewardCategory) {
        try {
            return RewardCategory.valueOf(rewardCategory.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(MissionErrorCode.MISSION_REWARD_CATEGORY_MISMATCH);
        }
    }

    /** 페이지 크기 정규화. */
    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_CURSOR_SIZE;
        }
        return Math.min(size, MAX_CURSOR_SIZE);
    }

    /** 커서 파싱. */
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
