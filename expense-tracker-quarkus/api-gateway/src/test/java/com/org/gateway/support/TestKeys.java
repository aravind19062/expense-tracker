package com.org.gateway.support;

import io.smallrye.jwt.build.Jwt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;

/** Throwaway key pairs + tokens shaped like auth-service's. Nothing is committed. */
public final class TestKeys {

    public static final String ISSUER = "https://expense-tracker.local/auth";
    public static final KeyPair TRUSTED = generate();
    public static final KeyPair UNTRUSTED = generate();

    private TestKeys() {
    }

    public static String validToken() {
        return Jwt.issuer(ISSUER).subject("101").upn("priya").groups(Set.of("user"))
                .expiresIn(3600).sign(TRUSTED.getPrivate());
    }

    public static String tokenWithGroups(String... groups) {
        return Jwt.issuer(ISSUER).subject("101").upn("priya").groups(Set.of(groups))
                .expiresIn(3600).sign(TRUSTED.getPrivate());
    }

    public static String forgedToken() {
        return Jwt.issuer(ISSUER).subject("101").groups(Set.of("user")).expiresIn(3600).sign(UNTRUSTED.getPrivate());
    }

    public static String expiredToken() {
        return Jwt.issuer(ISSUER).subject("101").groups(Set.of("user"))
                .issuedAt(java.time.Instant.now().minusSeconds(7200))
                .expiresAt(java.time.Instant.now().minusSeconds(3600))
                .sign(TRUSTED.getPrivate());
    }

    static Path writePublicKey() {
        try {
            Path file = Files.createDirectories(Path.of("target", "test-keys")).resolve("publicKey.pem");
            Files.writeString(file, "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(TRUSTED.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----\n");
            return file.toAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
