package com.project.domain.family.service.port;

import com.project.domain.family.entity.Family;

public interface FamilyEventPublisher {
    void publishFamilyCreated(Family family);
}
