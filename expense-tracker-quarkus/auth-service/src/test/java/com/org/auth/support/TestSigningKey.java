package com.org.auth.support;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/** Generates a throwaway RSA signing key under target/ for the test run; nothing is committed. */
public class TestSigningKey implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            String pem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(gen.generateKeyPair().getPrivate().getEncoded())
                    + "\n-----END PRIVATE KEY-----\n";
            Path file = Files.createDirectories(Path.of("target", "test-keys")).resolve("privateKey.pem");
            Files.writeString(file, pem);
            return Map.of("auth.jwt.private-key-location", file.toAbsolutePath().toString());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stop() {
    }
}
