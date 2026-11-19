package com.org.auth.service;

import com.org.auth.entity.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;

/** Replaces the Spring {@code JwtUtil} (HS256 + shared secret) with RS256 tokens built by SmallRye JWT Build. */
@ApplicationScoped
public class TokenService {

    private final SigningKeyProvider keys;
    private final String issuer;
    private final long lifespanSeconds;

    public TokenService(SigningKeyProvider keys,
                        @ConfigProperty(name = "auth.jwt.issuer") String issuer,
                        @ConfigProperty(name = "auth.jwt.lifespan-seconds", defaultValue = "3600") long lifespanSeconds) {
        this.keys = keys;
        this.issuer = issuer;
        this.lifespanSeconds = lifespanSeconds;
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        return Jwt.issuer(issuer)
                .subject(String.valueOf(user.id))
                .upn(user.email)
                .claim("email", user.email)
                .preferredUserName(user.userName)
                .groups(user.role.groups())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(lifespanSeconds))
                .jws().keyId(keys.keyId())
                .sign(keys.privateKey());
    }

    public long lifespanSeconds() {
        return lifespanSeconds;
    }
}
