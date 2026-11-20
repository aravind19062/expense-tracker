package com.org.gateway.error;

import java.time.Instant;

/** Same shape as the errors produced by auth-service and expense-service: {"code", "message", "timestamp"}. */
public record GatewayError(String code, String message, Instant timestamp) {

    public static GatewayError of(String code, String message) {
        return new GatewayError(code, message, Instant.now());
    }
}
