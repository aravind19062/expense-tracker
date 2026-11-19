package com.org.expense.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Throwaway RSA key pairs generated once per test JVM. Nothing is committed to the repository:
 * only the public key is written (to target/) so the service can load it like a real deployment.
 */
public final class TestKeys {

    /** Plays the role of auth-service's signing key. */
    public static final KeyPair TRUSTED = generate();
    /** A key the service does not trust - used to forge tokens. */
    public static final KeyPair UNTRUSTED = generate();

    private TestKeys() {
    }

    public static Path writeTrustedPublicKeyPem() {
        try {
            Path dir = Files.createDirectories(Path.of("target", "test-keys"));
            Path file = dir.resolve("publicKey.pem");
            String pem = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(TRUSTED.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----\n";
            Files.writeString(file, pem);
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
