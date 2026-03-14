package com.project.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.project.common.util.BaseEntity;
import com.project.domain.reward.enums.RewardCategory;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "reward_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class RewardTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private RewardCategory category;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    public RewardTemplate(
            Long id,
            String name,
            RewardCategory category,
            String thumbnailUrl,
            Integer price,
            boolean isSystem,
            boolean isActive) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.price = price;
        this.isSystem = isSystem;
        this.isActive = isActive;
    }

    public void update(String name, String thumbnailUrl, Integer price, boolean isActive) {
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.price = price;
        this.isActive = isActive;
    }

    public void delete() {
        softDelete();
    }
}
