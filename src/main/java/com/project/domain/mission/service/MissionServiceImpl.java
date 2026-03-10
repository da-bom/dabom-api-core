package com.project.domain.mission.service;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.project.domain.mission.enums.MissionLogActionType;
import com.project.domain.mission.enums.MissionRequestStatus;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.model.CreateMissionResult;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.model.MissionLogListResult;
import com.project.domain.mission.model.MissionRequestResult;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.MissionLogRepository;
import com.project.domain.mission.repository.MissionRequestRepository;
import com.project.domain.reward.entity.Reward;
import com.project.domain.reward.service.RewardSnapshotService;
import com.project.domain.reward.support.RewardDtoMapper;
import com.project.global.auth.model.AuthContext;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.MissionErrorCode;

import lombok.RequiredArgsConstructor;

/** 미션 조회, 생성, 취소, 완료 요청을 처리하는 서비스 구현체. */
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
    private final RewardSnapshotService rewardSnapshotService;
    private final CustomerRepository customerRepository;
    private final FamilyMemberRepository familyMemberRepository;

    /** 역할 범위 내 활성 미션 중 PENDING 또는 요청 이력이 없는 미션만 조회한다. */
    @Override
    public MissionListResult listMissions(AuthContext auth, String cursor, int size) {

        // 1. 요청 파라미터를 커서 기반 조회에 사용할 값으로 정규화한다.
        int pageSize = normalizeSize(size);
        Long cursorId = parseCursor(cursor);

        // 2. 목록 정책에 맞는 미션만 별도 수집해 페이지 조각을 만든다.
        VisibleMissionSlice visibleMissionSlice = findVisibleMissionSlice(auth, cursorId, pageSize);

        // 3. 최종 페이지에 포함된 미션만 기준으로 사용자 이름을 조회해 응답을 조합한다.
        Map<Long, String> customerNameMap = loadCustomerNameMap(visibleMissionSlice.missions());
        return new MissionListResult(
                visibleMissionSlice.missions().stream()
                        .map(
                                mission ->
                                        toMissionCard(
                                                mission,
                                                customerNameMap,
                                                visibleMissionSlice.requestStatusMap()))
                        .toList(),
                visibleMissionSlice.nextCursor(),
                visibleMissionSlice.hasNext());
    }

    /** 권한 범위에 맞는 미션 로그 목록을 커서 기반으로 조회한다. */
    @Override
    public MissionLogListResult listMissionLogs(AuthContext auth, String cursor, int size) {
        // 1. 커서 입력을 정규화한다.
        int pageSize = normalizeSize(size);
        Long cursorId = parseCursor(cursor);

        // 2. OWNER는 가족 기준, MEMBER는 본인 기준으로 로그를 조회한다.
        List<MissionLog> logs =
                auth.isOwner()
                        ? missionLogRepository.findByFamilyScope(
                                auth.familyId(), cursorId, PageRequest.of(0, pageSize + 1))
                        : missionLogRepository.findByTargetScope(
                                auth.customerId(), cursorId, PageRequest.of(0, pageSize + 1));

        boolean hasNext = logs.size() > pageSize;
        List<MissionLog> page = hasNext ? logs.subList(0, pageSize) : logs;
        String nextCursor = hasNext ? String.valueOf(page.getLast().getId()) : null;

        // 3. 로그에 연결된 미션과 사용자명을 조회해 응답 모델로 조합한다.
        Map<Long, MissionItem> missionMap =
                missionItemRepository
                        .findAllWithRewardByIdIn(
                                page.stream()
                                        .map(MissionLog::getMissionItemId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(MissionItem::getId, Function.identity()));
        Map<Long, String> customerNameMap = loadCustomerNameMapFromLogs(page, missionMap);

        List<MissionLogListResult.MissionLogItem> items =
                page.stream()
                        .map(log -> toMissionLogItem(log, missionMap, customerNameMap))
                        .toList();
        return new MissionLogListResult(items, nextCursor, hasNext);
    }

    /** OWNER가 미션을 생성하고 Reward 스냅샷을 함께 저장한다. */
    @Override
    @Transactional
    public CreateMissionResult createMission(AuthContext auth, CreateMissionRequest req) {
        // 1. 요청자가 OWNER인지, 대상이 같은 가족의 MEMBER인지 검증한다.
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

        // 2. 템플릿을 조회한 뒤 현재 값을 복사한 Reward 스냅샷을 생성한다.
        Reward reward =
                rewardSnapshotService.createFromTemplate(req.rewardTemplateId(), req.rewardValue());

        // 생성된 Reward를 미션에 연결해 저장한다.
        MissionItem mission =
                missionItemRepository.save(
                        MissionItem.builder()
                                .familyId(auth.familyId())
                                .targetCustomerId(req.targetCustomerId())
                                .createdById(auth.customerId())
                                .reward(reward)
                                .missionText(req.missionText())
                                .status(MissionStatus.ACTIVE)
                                .build());

        appendLog(
                mission.getId(),
                auth.customerId(),
                MissionLogActionType.CREATED,
                "Mission created");
        return new CreateMissionResult(mission.getId(), mission.getCreatedAt());
    }

    /** OWNER가 활성 미션을 취소한다. */
    @Override
    @Transactional
    public void cancelMission(AuthContext auth, Long missionId) {
        assertOwner(auth);
        MissionItem mission = findMissionByFamilyScopeForUpdate(auth, missionId);
        if (!mission.canCancel()) {
            throw new ApplicationException(MissionErrorCode.MISSION_INVALID_STATUS_TRANSITION);
        }

        mission.cancel();
        appendLog(
                mission.getId(),
                auth.customerId(),
                MissionLogActionType.CANCELLED,
                "Mission cancelled");
    }

    /** MEMBER가 본인에게 할당된 미션의 완료 승인을 요청한다. */
    @Override
    @Transactional
    public MissionRequestResult requestMissionApproval(AuthContext auth, Long missionId) {
        // 1. 미션 할당 대상과 상태, 중복 요청 여부를 검증한다.
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

        // 2. 중복 생성 경쟁을 대비해 요청 저장 예외를 도메인 예외로 변환한다.
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

        // 3. 응답에는 MissionItem이 참조하는 Reward 스냅샷을 그대로 사용한다.
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
                        RewardDtoMapper.toModel(mission.getReward())),
                request.getStatus().name(),
                new MissionListResult.CustomerSummary(auth.customerId(), requesterName),
                request.getCreatedAt());
    }

    /** 역할에 따라 활성 미션 조회 범위를 분기한다. */
    private List<MissionItem> findActiveMissionItemsByRole(
            AuthContext auth, Long cursorId, int fetchSize) {
        if (auth.isOwner()) {
            return missionItemRepository.findByFamilyScope(
                    auth.familyId(), MissionStatus.ACTIVE, cursorId, PageRequest.of(0, fetchSize));
        }
        return missionItemRepository.findByTargetScope(
                auth.customerId(), MissionStatus.ACTIVE, cursorId, PageRequest.of(0, fetchSize));
    }

    /** 목록 정책에 맞는 미션만 수집해 커서 페이지 조각으로 반환한다. */
    private VisibleMissionSlice findVisibleMissionSlice(
            AuthContext auth, Long cursorId, int pageSize) {
        // 1. 활성 미션 자체는 커서 기준으로 읽고, 노출 가능한 미션만 별도 버퍼에 담는다.
        List<MissionItem> visibleMissions = new ArrayList<>();
        Map<Long, String> visibleRequestStatusMap = new HashMap<>();
        Long nextFetchCursor = cursorId;
        int fetchSize = pageSize + 1;
        boolean sourceExhausted = false;

        while (visibleMissions.size() < fetchSize && !sourceExhausted) {
            // 2. 한 번 읽은 chunk에 대해 최신 요청 상태를 붙인 뒤 노출 여부를 판단한다.
            List<MissionItem> chunk =
                    findActiveMissionItemsByRole(auth, nextFetchCursor, fetchSize);
            if (chunk.isEmpty()) {
                sourceExhausted = true;
            } else {
                // 3. 목록 노출 조건에 맞는 미션만 골라서 따로 담습니다
                collectVisibleMissions(chunk, fetchSize, visibleMissions, visibleRequestStatusMap);

                nextFetchCursor = chunk.getLast().getId();
                sourceExhausted = chunk.size() < fetchSize;
            }
        }

        // 4. pageSize + 1개 수집 결과를 바탕으로 다음 페이지 존재 여부를 계산한다.
        return toVisibleMissionSlice(pageSize, visibleMissions, visibleRequestStatusMap);
    }

    /** 목록에는 PENDING 상태이거나 요청 이력이 없는 미션만 노출한다. */
    private boolean isVisibleMissionRequestStatus(String requestStatus) {
        return requestStatus == null || MissionRequestStatus.PENDING.name().equals(requestStatus);
    }

    /** 한 번 조회한 chunk에서 목록 노출 조건을 만족하는 미션만 누적한다. */
    private void collectVisibleMissions(
            List<MissionItem> chunk,
            int fetchSize,
            List<MissionItem> visibleMissions,
            Map<Long, String> visibleRequestStatusMap) {
        Map<Long, String> chunkRequestStatusMap = loadMissionRequestStatusMap(chunk);
        for (MissionItem mission : chunk) {
            if (visibleMissions.size() == fetchSize) {
                return;
            }

            String requestStatus = chunkRequestStatusMap.get(mission.getId());
            if (isVisibleMissionRequestStatus(requestStatus)) {
                visibleMissions.add(mission);
                if (requestStatus != null) {
                    visibleRequestStatusMap.put(mission.getId(), requestStatus);
                }
            }
        }
    }

    /** 누적된 미션 목록을 pageSize 기준의 커서 페이지 조각으로 변환한다. */
    private VisibleMissionSlice toVisibleMissionSlice(
            int pageSize,
            List<MissionItem> visibleMissions,
            Map<Long, String> visibleRequestStatusMap) {
        boolean hasNext = visibleMissions.size() > pageSize;
        List<MissionItem> page = hasNext ? visibleMissions.subList(0, pageSize) : visibleMissions;
        Map<Long, String> pageRequestStatusMap =
                page.stream()
                        .filter(mission -> visibleRequestStatusMap.containsKey(mission.getId()))
                        .collect(
                                Collectors.toMap(
                                        MissionItem::getId,
                                        mission -> visibleRequestStatusMap.get(mission.getId())));
        String nextCursor = hasNext ? String.valueOf(page.getLast().getId()) : null;
        return new VisibleMissionSlice(page, pageRequestStatusMap, nextCursor, hasNext);
    }

    private MissionListResult.MissionCard toMissionCard(
            MissionItem mission,
            Map<Long, String> customerNameMap,
            Map<Long, String> requestStatusMap) {
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
                RewardDtoMapper.toModel(mission.getReward()),
                mission.getCreatedAt());
    }

    /** 미션별 최신 요청 상태를 맵 형태로 로드한다. */
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

    /** 미션 로그 엔티티를 응답 모델로 변환한다. */
    private MissionLogListResult.MissionLogItem toMissionLogItem(
            MissionLog log, Map<Long, MissionItem> missionMap, Map<Long, String> customerNameMap) {
        MissionItem mission = missionMap.get(log.getMissionItemId());
        if (mission == null) {
            throw new ApplicationException(MissionErrorCode.MISSION_NOT_FOUND);
        }
        return new MissionLogListResult.MissionLogItem(
                log.getId(),
                log.getActionType().name(),
                log.getMessage(),
                new MissionLogListResult.MissionItemSimple(
                        mission.getId(),
                        mission.getMissionText(),
                        RewardDtoMapper.toModel(mission.getReward())),
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

    /** 미션 목록 응답에 필요한 사용자명을 일괄 조회한다. */
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

    /** 미션 로그 응답에 필요한 사용자명을 일괄 조회한다. */
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

    /** 가족 범위 내 미션을 잠금과 함께 조회한다. */
    private MissionItem findMissionByFamilyScopeForUpdate(AuthContext auth, Long missionId) {
        return missionItemRepository
                .findByIdAndFamilyIdForUpdate(missionId, auth.familyId())
                .orElseThrow(() -> new ApplicationException(MissionErrorCode.MISSION_NOT_FOUND));
    }

    /** 미션 이력 추적을 위해 로그를 추가한다. */
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

    /** 노출 미션 목록과 요청 상태, 다음 커서 정보를 담는다. */
    private record VisibleMissionSlice(
            List<MissionItem> missions,
            Map<Long, String> requestStatusMap,
            String nextCursor,
            boolean hasNext) {}
}
