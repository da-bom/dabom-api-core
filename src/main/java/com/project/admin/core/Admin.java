package com.project.admin.core;

import lombok.Builder;
import lombok.Getter;

/** 관리자 도메인 */
@Getter
@Builder
public class Admin {
    private final Long adminId;
    private final String email;
    private final String name;
    private final String passwordHash;

    public static Admin withId(Long adminId, String email, String name, String passwordHash) {
        return Admin.builder()
                .adminId(adminId)
                .email(email)
                .name(name)
                .passwordHash(passwordHash)
                .build();
    }

    public void validatePassword(String password) {
        AdminRule.validatePassword(password, passwordHash);
    }
}
