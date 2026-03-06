package com.project.domain.mission.repository;

import static com.project.domain.mission.entity.QMissionItem.missionItem;
import static com.project.domain.mission.entity.QRewardTemplate.rewardTemplate;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.project.domain.mission.enums.MissionStatus;
import com.project.domain.mission.repository.projection.MissionListRow;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MissionItemRepositoryImpl implements MissionItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MissionListRow> findMissionRowsByFamilyIdAndStatus(Long familyId, MissionStatus status) {
        return queryFactory
                .select(
                        missionItem.id,
                        missionItem.missionText,
                        missionItem.status,
                        rewardTemplate.id,
                        rewardTemplate.name,
                        rewardTemplate.category,
                        rewardTemplate.unit,
                        missionItem.rewardValue,
                        missionItem.createdAt)
                .from(missionItem)
                .join(rewardTemplate)
                .on(missionItem.rewardTemplateId.eq(rewardTemplate.id))
                .where(
                        missionItem.familyId.eq(familyId),
                        statusEq(status))
                .orderBy(missionItem.createdAt.desc())
                .fetch()
                .stream()
                .map(
                        tuple ->
                                new MissionListRow(
                                        tuple.get(missionItem.id),
                                        tuple.get(missionItem.missionText),
                                        tuple.get(missionItem.status),
                                        tuple.get(rewardTemplate.id),
                                        tuple.get(rewardTemplate.name),
                                        tuple.get(rewardTemplate.category),
                                        tuple.get(rewardTemplate.unit),
                                        tuple.get(missionItem.rewardValue),
                                        tuple.get(missionItem.createdAt)))
                .toList();
    }

    private BooleanExpression statusEq(MissionStatus status) {
        if (status == null) {
            return null;
        }
        return missionItem.status.eq(status);
    }
}
