package com.project.domain.family.model;

import com.project.common.auth.enums.RoleType;

public record FamilyMemberInfo(Long customerId, String name, RoleType role) {}
