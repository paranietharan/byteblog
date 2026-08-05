package com.paranietharan.byteblog.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
    ADMIN("Admin role - full access"),
    USER("User role - limited access");

    private final String description;
}
