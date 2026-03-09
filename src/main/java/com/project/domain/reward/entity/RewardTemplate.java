package com.project.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.project.domain.reward.enums.RewardCategory;
import com.project.global.util.BaseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reward_template")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RewardTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardCategory category;

    @Column(name = "default_value", nullable = false)
    private Long defaultValue;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "is_system", columnDefinition = "tinyint", nullable = false)
    private boolean isSystem;

    public void update(
            String name,
            RewardCategory category,
            Long defaultValue,
            String unit,
            boolean isSystem) {
        this.name = name;
        this.category = category;
        this.defaultValue = defaultValue;
        this.unit = unit;
        this.isSystem = isSystem;
    }

    public void delete() {
        softDelete();
    }
}
