package com.project.domain.reward.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.reward.dto.request.RewardTemplateRequest;
import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.enums.RewardCategory;
import com.project.domain.reward.repository.RewardTemplateRepository;
import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.RewardErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardTemplateServiceImpl implements RewardTemplateService {

    private final RewardTemplateRepository rewardTemplateRepository;

    @Override
    public List<RewardTemplate> getAllTemplates() {
        return rewardTemplateRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public List<RewardTemplate> getActiveTemplates(RewardCategory category) {
        return rewardTemplateRepository
                .findAllByDeletedAtIsNullAndIsActiveTrueAndCategoryOrderByIdAsc(category);
    }

    @Override
    public RewardTemplate getTemplate(Long id) {
        return findTemplateOrThrow(id);
    }

    @Override
    @Transactional
    public RewardTemplate createTemplate(RewardTemplateRequest.Create request) {
        RewardTemplate template =
                RewardTemplate.builder()
                        .name(request.name())
                        .category(request.category())
                        .thumbnailUrl(request.thumbnailUrl())
                        .price(request.price())
                        .isSystem(false)
                        .isActive(true)
                        .build();
        return rewardTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public RewardTemplate updateTemplate(Long id, RewardTemplateRequest.Update request) {
        RewardTemplate template = findTemplateOrThrow(id);
        template.update(
                request.name(), request.thumbnailUrl(), request.price(), request.isActive());
        return template;
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        RewardTemplate template = findTemplateOrThrow(id);
        if (template.isSystem()) {
            throw new ApplicationException(RewardErrorCode.REWARD_TEMPLATE_SYSTEM_DELETE);
        }
        template.delete();
    }

    private RewardTemplate findTemplateOrThrow(Long id) {
        return rewardTemplateRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(
                        () -> new ApplicationException(RewardErrorCode.REWARD_TEMPLATE_NOT_FOUND));
    }
}
