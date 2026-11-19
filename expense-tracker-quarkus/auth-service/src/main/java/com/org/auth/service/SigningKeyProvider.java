package com.org.auth.service;

import io.quarkus.runtime.Startup;
import io.smallrye.jwt.util.KeyUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

/**
 * Loads the RSA private key used to sign JWTs from a file outside the repository
 * (mounted secret in Docker, ./keys in local dev) and derives the matching public key,
 * which is published as a JWKS so other services can verify signatures.
 * Fails fast at startup if the key is missing or not an RSA key.
 */
@Startup
@ApplicationScoped
public class SigningKeyProvider {

    private final PrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;
    private final String jwksJson;

    public SigningKeyProvider(@ConfigProperty(name = "auth.jwt.private-key-location") String location) {
        try {
            String pem = Files.readString(Path.of(location));
            this.privateKey = KeyUtils.decodePrivateKey(pem);
            if (!(privateKey instanceof RSAPrivateCrtKey crt)) {
                throw new IllegalStateException("JWT signing key at " + location + " is not an RSA (PKCS#8) private key");
            }
            this.publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));

            RsaJsonWebKey jwk = new RsaJsonWebKey(publicKey);
            this.keyId = jwk.calculateBase64urlEncodedThumbprint("SHA-256");
            jwk.setKeyId(keyId);
            jwk.setUse("sig");
            jwk.setAlgorithm("RS256");
            this.jwksJson = new JsonWebKeySet(jwk).toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read JWT signing key from " + location, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid JWT signing key at " + location, e);
        }
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    public String keyId() {
        return keyId;
    }

    public String jwksJson() {
        return jwksJson;
    }
}
