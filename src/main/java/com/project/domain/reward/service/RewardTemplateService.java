package com.project.domain.reward.service;

import java.util.List;

import com.project.domain.reward.dto.request.RewardTemplateRequest;
import com.project.domain.reward.entity.RewardTemplate;

public interface RewardTemplateService {

    List<RewardTemplate> getAllTemplates();

    RewardTemplate createTemplate(RewardTemplateRequest.Upsert request);

    RewardTemplate updateTemplate(Long id, RewardTemplateRequest.Upsert request);

    void deleteTemplate(Long id);
}
