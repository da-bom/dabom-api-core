package com.project.customer.infra.entity;

import java.time.LocalDateTime;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class BaseJpaEntity {
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
