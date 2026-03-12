package com.project.domain.usagerecord.infra.event;

import com.dabom.messaging.kafka.event.dto.usage.UsageRealtimePayload;

public record RealtimeUsageMemberEvent(UsageRealtimePayload payload) {}
