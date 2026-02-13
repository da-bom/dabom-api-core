package com.project.admin.core;

import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.AdminErrorCode;

/** 관리자 도메인 규칙 */
public final class AdminRule {
    private AdminRule() {
        // static utility
    }

    public static void validatePassword(String password, String passwordHash) {
        if (password == null || !password.equals(passwordHash)) {
            throw new ApplicationException(AdminErrorCode.ADMIN_SIGN_IN_FAILED);
        }
    }
}
