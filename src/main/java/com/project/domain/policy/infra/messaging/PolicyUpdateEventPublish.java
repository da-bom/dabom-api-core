package com.project.domain.policy.infra.messaging;

import com.project.global.event.dto.policy.PolicyUpdatedPayload;

public interface PolicyUpdateEventPublish {
    void publish(PolicyUpdatedPayload payload);
}
