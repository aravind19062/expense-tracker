package com.org.gateway.error;

import java.time.Instant;

/** Same shape as the error bodies produced by auth-service and expense-service. */
public record GatewayError(Instant timestamp, int status, String error, String message, String path) {

    public static GatewayError of(int status, String error, String message, String path) {
        return new GatewayError(Instant.now(), status, error, message, path);
    }
}
