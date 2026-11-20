package com.org.gateway;

import com.org.gateway.support.DownstreamMocks;
import com.org.gateway.support.TestKeys;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/** expense-service is registered in Stork at a port where nothing is listening (connection refused). */
@QuarkusTest
@WithTestResource(value = DownstreamMocks.class, initArgs = @ResourceArg(name = "expense", value = "down"))
class DownstreamUnavailableTest {

    @Test
    void unreachableExpenseServiceReturns503Json() {
        given().auth().oauth2(TestKeys.validToken()).get("/api/expenses")
                .then().statusCode(503)
                .body("code", equalTo("SERVICE_UNAVAILABLE"))
                .body("message", equalTo("Downstream service is unavailable, please retry later"))
                .body("timestamp", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void authFailureIsStillReportedBeforeAnyDownstreamCall() {
        given().get("/api/expenses").then().statusCode(401);
    }
}
