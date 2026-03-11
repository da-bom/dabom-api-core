package com.project.domain.reward.dto.response;

import java.time.LocalDateTime;

import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.enums.RewardCategory;

public class RewardTemplateResponse {

    public record Detail(
            Long id,
            String name,
            RewardCategory category,
            String thumbnailUrl,
            Integer price,
            boolean isSystem,
            boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static Detail from(RewardTemplate template) {
            return new Detail(
                    template.getId(),
                    template.getName(),
                    template.getCategory(),
                    template.getThumbnailUrl(),
                    template.getPrice(),
                    template.isSystem(),
                    template.isActive(),
                    template.getCreatedAt(),
                    template.getUpdatedAt());
        }
    }
}
