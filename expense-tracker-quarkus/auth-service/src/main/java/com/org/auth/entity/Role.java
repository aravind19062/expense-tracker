package com.org.auth.entity;

import java.util.Set;

public enum Role {
    USER, ADMIN;

    /** JWT "groups" claim for this role. ADMIN implies USER so admins can use the normal API. */
    public Set<String> groups() {
        return this == ADMIN ? Set.of(ADMIN.name(), USER.name()) : Set.of(USER.name());
    }
}
