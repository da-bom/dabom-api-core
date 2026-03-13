package com.project.domain.family.model;

import com.project.domain.customer.enums.RoleType;

public record FamilyMemberInfo(Long customerId, String name, RoleType role) {}
