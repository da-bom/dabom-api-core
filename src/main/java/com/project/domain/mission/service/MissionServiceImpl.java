package com.project.domain.mission.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.family.service.FamilyService;
import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.model.MissionListResult;
import com.project.domain.mission.repository.MissionItemRepository;
import com.project.domain.mission.repository.projection.MissionListRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionServiceImpl implements MissionService {

    private final MissionItemRepository missionItemRepository;
    private final FamilyService familyService;

    @Override
    public MissionListResult getMissions(Long customerId, MissionStatus status) {
        Long familyId = familyService.getFamilyIdByCustomerId(customerId);
        List<MissionListRow> missionRows =
                missionItemRepository.findMissionRowsByFamilyIdAndStatus(familyId, status);

        List<MissionListResult.MissionDetail> missions =
                missionRows.stream()
                        .map(
                                row ->
                                        new MissionListResult.MissionDetail(
                                                row.missionItemId(),
                                                row.missionText(),
                                                row.status(),
                                                new MissionListResult.RewardTemplateDetail(
                                                        row.rewardTemplateId(),
                                                        row.rewardTemplateName(),
                                                        row.rewardCategory(),
                                                        row.rewardUnit()),
                                                row.rewardValue(),
                                                row.createdAt()))
                        .toList();

        return new MissionListResult(missions);
    }
}
