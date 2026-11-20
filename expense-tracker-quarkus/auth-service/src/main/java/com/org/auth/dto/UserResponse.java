package com.org.auth.dto;

import com.org.auth.entity.User;

/** Returned by POST /api/auth/register. Never contains the password hash. */
public record UserResponse(Long id, String username, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id, user.username, user.email);
    }
}
