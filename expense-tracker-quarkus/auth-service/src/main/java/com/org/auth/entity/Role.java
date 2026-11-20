package com.org.auth.entity;

import java.util.Set;

public enum Role {
    USER, ADMIN;

    /** JWT "groups" claim: lower-case group names, e.g. ["user"]. ADMIN implies user so admins can use the normal API. */
    public Set<String> groups() {
        return this == ADMIN ? Set.of("admin", "user") : Set.of("user");
    }
}
