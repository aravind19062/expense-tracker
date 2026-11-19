package com.org.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "User name is required")
        @Size(max = 100, message = "User name must be at most 100 characters")
        String userName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        // BCrypt only uses the first 72 bytes of input, so longer passwords are rejected.
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
        String password) {

    @Override
    public String toString() {
        return "RegisterRequest[userName=" + userName + ", email=" + email + ", password=****]";
    }
}
