package com.project.admin.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import com.project.customer.infra.entity.BaseJpaEntity;

@Entity
public class AdminJpaEntity extends BaseJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String email;

    @Column private String name;

    @Column(nullable = false, length = 20)
    private String passwordHash;

    public Long getId() {
        return id;
    }

    public void validatePassword(String password) {
        if (!password.equals(passwordHash)) {
            throw new IllegalArgumentException("Invalid password");
        }
    }
}
