package com.org.auth.dto;

/** {@code token} is the field the original API returned; {@code expiresIn} (seconds) is additive. */
public record LoginResponse(String token, long expiresIn) {
}
