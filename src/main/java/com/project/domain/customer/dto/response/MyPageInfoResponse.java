package com.project.domain.customer.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.domain.customer.model.MyPageInfo;

public record MyPageInfoResponse(
        String name,
        String familyName,
        boolean isBlocked,
        String blockReason,
        Long monthlyLimitBytes,
        Long monthlyUsedBytes,
        JsonNode timeBlock) {

    public static MyPageInfoResponse from(MyPageInfo myPageInfo) {
        return new MyPageInfoResponse(
                myPageInfo.name(),
                myPageInfo.familyName(),
                myPageInfo.isBlocked(),
                myPageInfo.blockReason(),
                myPageInfo.monthlyLimitBytes(),
                myPageInfo.monthlyUsedBytes(),
                myPageInfo.timeBlock());
    }
}
