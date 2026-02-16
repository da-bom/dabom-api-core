package com.project.domain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.AdminErrorCode;
import com.project.global.util.BaseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String email;

    @Column private String name;

    @Column(nullable = false, length = 20)
    private String passwordHash;

    public void validatePassword(String password) {
        if (password == null || !password.equals(passwordHash)) {
            throw new ApplicationException(AdminErrorCode.ADMIN_SIGN_IN_FAILED);
        }
    }
}
