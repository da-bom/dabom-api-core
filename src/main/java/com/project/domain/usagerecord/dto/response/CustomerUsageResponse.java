package com.project.domain.usagerecord.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUsageResponse {

    private Long customerId;
    private String name;

    private Long monthlyUsedBytes;
    private Long monthlyLimitBytes;

    private Boolean isMe;
}
