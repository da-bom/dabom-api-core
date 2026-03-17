package com.project.domain.admin.dto.response;

import com.project.domain.admin.entity.Admin;

public record AdminMeResponse(Long adminId, String email, String name) {

    public static AdminMeResponse from(Admin admin) {
        return new AdminMeResponse(admin.getId(), admin.getEmail(), admin.getName());
    }
}
