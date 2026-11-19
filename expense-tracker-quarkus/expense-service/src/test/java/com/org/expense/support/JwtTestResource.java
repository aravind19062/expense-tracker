package com.org.expense.support;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

/** Points the service's JWT verification at the throwaway test public key. */
public class JwtTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return Map.of("mp.jwt.verify.publickey.location", TestKeys.writeTrustedPublicKeyPem().toString());
    }

    @Override
    public void stop() {
    }
}
