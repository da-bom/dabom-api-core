package com.project.domain.reward.service;

import java.util.List;

import com.project.domain.reward.dto.request.RewardTemplateRequest;
import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.enums.RewardCategory;

public interface RewardTemplateService {

    List<RewardTemplate> getAllTemplates();

    List<RewardTemplate> getActiveTemplates(RewardCategory category);

    RewardTemplate getTemplate(Long id);

    RewardTemplate createTemplate(RewardTemplateRequest.Create request);

    RewardTemplate updateTemplate(Long id, RewardTemplateRequest.Update request);

    void deleteTemplate(Long id);
}
