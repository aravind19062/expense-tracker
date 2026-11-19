package com.org.gateway.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Starts WireMock servers that play auth-service and expense-service, and registers their
 * addresses in the gateway's Stork static service lists - so the real discovery path is exercised.
 * With init arg {@code expense=down}, expense-service is pointed at a closed port instead.
 */
public class DownstreamMocks implements QuarkusTestResourceLifecycleManager {

    public static WireMockServer auth;
    public static WireMockServer expense;
    private boolean expenseDown;

    @Override
    public void init(Map<String, String> initArgs) {
        expenseDown = "down".equals(initArgs.get("expense"));
    }

    @Override
    public Map<String, String> start() {
        auth = new WireMockServer(wireMockConfig().dynamicPort());
        auth.start();
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.stork.auth-service.service-discovery.address-list", "localhost:" + auth.port());
        config.put("mp.jwt.verify.publickey.location", TestKeys.writePublicKey().toString());

        if (expenseDown) {
            config.put("quarkus.stork.expense-service.service-discovery.address-list", "localhost:" + closedPort());
        } else {
            expense = new WireMockServer(wireMockConfig().dynamicPort());
            expense.start();
            config.put("quarkus.stork.expense-service.service-discovery.address-list", "localhost:" + expense.port());
        }
        return config;
    }

    @Override
    public void stop() {
        if (auth != null) {
            auth.stop();
        }
        if (expense != null) {
            expense.stop();
        }
    }

    private static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort(); // released immediately -> nothing listens there
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
