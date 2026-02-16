package com.project.domain.usagerecord.infra.event;

import com.project.global.event.dto.usage.UsageRealtimePayload;

public record RealtimeUsageMemberEvent(UsageRealtimePayload payload) {}
