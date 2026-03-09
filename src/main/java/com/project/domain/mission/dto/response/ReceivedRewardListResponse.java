package com.project.domain.mission.dto.response;

import java.util.List;

import com.project.domain.mission.model.ReceivedRewardListResult;

/** 보상 수령 내역 목록 응답 DTO */
public record ReceivedRewardListResponse(
        List<ReceivedRewardItemResponse> rewards, String nextCursor, boolean hasNext) {
    public static ReceivedRewardListResponse from(ReceivedRewardListResult result) {
        return new ReceivedRewardListResponse(
                result.rewards().stream().map(ReceivedRewardItemResponse::from).toList(),
                result.nextCursor(),
                result.hasNext());
    }
}
