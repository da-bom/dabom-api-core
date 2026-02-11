package com.project.customer.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class CustomerJpaEntity extends BaseJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    private String passwordHash;

    @Column(nullable = false, length = 10)
    private String name;

    public Long getId() {
        return id;
    }

    public void validatePassword(String password) {
        if (!password.equals(passwordHash)) {
            throw new IllegalArgumentException("Invalid password");
        }
    }
}
