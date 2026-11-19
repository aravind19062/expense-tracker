package com.org.auth;

import com.org.auth.service.SigningKeyProvider;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Plain unit test: the service must refuse to start without a valid signing key. */
class SigningKeyProviderTest {

    @Test
    void missingKeyFileFailsFast() {
        assertThrows(UncheckedIOException.class, () -> new SigningKeyProvider("/definitely/not/here.pem"));
    }

    @Test
    void garbageKeyFileFailsFast() throws Exception {
        Path file = Files.createTempFile("bad-key", ".pem");
        Files.writeString(file, "-----BEGIN PRIVATE KEY-----\nbm90IGEga2V5\n-----END PRIVATE KEY-----\n");
        assertThrows(RuntimeException.class, () -> new SigningKeyProvider(file.toString()));
    }
}
