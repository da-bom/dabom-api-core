package com.project.domain.reward.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.reward.dto.request.RewardTemplateRequest;
import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.repository.RewardTemplateRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.RewardErrorCode;

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
    @Transactional
    public RewardTemplate createTemplate(RewardTemplateRequest.Create request) {
        RewardTemplate template =
                RewardTemplate.builder()
                        .name(request.name())
                        .category(request.category())
                        .defaultValue(request.defaultValue())
                        .unit(request.unit())
                        .isSystem(request.isSystem())
                        .build();
        return rewardTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public RewardTemplate updateTemplate(Long id, RewardTemplateRequest.Update request) {
        RewardTemplate template = findTemplateOrThrow(id);
        template.update(
                request.name(),
                request.category(),
                request.defaultValue(),
                request.unit(),
                request.isSystem());
        return template;
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        RewardTemplate template = findTemplateOrThrow(id);
        template.delete();
    }

    private RewardTemplate findTemplateOrThrow(Long id) {
        return rewardTemplateRepository
                .findById(id)
                .orElseThrow(
                        () -> new ApplicationException(RewardErrorCode.REWARD_TEMPLATE_NOT_FOUND));
    }
}
