package com.org.expense.support;

import io.smallrye.jwt.build.Jwt;

import java.time.Instant;
import java.util.Set;

/** Builds tokens shaped exactly like the ones auth-service issues. */
public final class Tokens {

    public static final String ISSUER = "https://expense-tracker.local/auth";

    private Tokens() {
    }

    public static String validFor(String userId) {
        return base(userId).groups(Set.of("user")).sign(TestKeys.TRUSTED.getPrivate());
    }

    public static String expiredFor(String userId) {
        return Jwt.issuer(ISSUER).subject(userId).groups(Set.of("user"))
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .sign(TestKeys.TRUSTED.getPrivate());
    }

    public static String signedWithUntrustedKey(String userId) {
        return base(userId).groups(Set.of("user")).sign(TestKeys.UNTRUSTED.getPrivate());
    }

    public static String wrongIssuer(String userId) {
        return Jwt.issuer("https://evil.example.com").subject(userId).groups(Set.of("user"))
                .expiresIn(3600).sign(TestKeys.TRUSTED.getPrivate());
    }

    public static String withoutUserRole(String userId) {
        return base(userId).groups(Set.of("guest")).sign(TestKeys.TRUSTED.getPrivate());
    }

    private static io.smallrye.jwt.build.JwtClaimsBuilder base(String userId) {
        return Jwt.issuer(ISSUER).subject(userId).upn(userId + "@example.com").expiresIn(3600);
    }
}
