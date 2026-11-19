package com.org.gateway;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.org.gateway.support.DownstreamMocks;
import com.org.gateway.support.TestKeys;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.org.gateway.support.DownstreamMocks.auth;
import static com.org.gateway.support.DownstreamMocks.expense;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@WithTestResource(DownstreamMocks.class)
class GatewayRoutingTest {

    private static final String EXPENSE_JSON =
            "{\"id\":7,\"title\":\"Weekly groceries\",\"amount\":82.45,\"categoryId\":3,\"categoryName\":\"Groceries\"}";

    @BeforeEach
    void resetMocks() {
        auth.resetAll();
        expense.resetAll();
    }

    @Nested
    class PublicAuthRoutes {

        @Test
        void registrationIsPublicAndRelaysDownstreamResponse() {
            auth.stubFor(post("/auth/register").willReturn(aResponse().withStatus(201)
                    .withHeader("Content-Type", "text/plain").withBody("User registered successfully")));

            given().contentType(ContentType.JSON)
                    .body("{\"userName\":\"Priya\",\"email\":\"priya@example.com\",\"password\":\"S3cure!pass\"}")
                    .post("/auth/register")
                    .then().statusCode(201)
                    .contentType(startsWith("text/plain"))
                    .body(org.hamcrest.Matchers.equalTo("User registered successfully"));

            auth.verify(1, postRequestedFor(urlEqualTo("/auth/register"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withRequestBody(equalToJson("{\"userName\":\"Priya\",\"email\":\"priya@example.com\",\"password\":\"S3cure!pass\"}")));
        }

        @Test
        void loginIsPublicAndReturnsToken() {
            auth.stubFor(post("/auth/login").willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json").withBody("{\"token\":\"abc.def.ghi\",\"expiresIn\":3600}")));

            given().contentType(ContentType.JSON).body("{\"email\":\"priya@example.com\",\"password\":\"S3cure!pass\"}")
                    .post("/auth/login")
                    .then().statusCode(200)
                    .body("token", org.hamcrest.Matchers.equalTo("abc.def.ghi"))
                    .body("expiresIn", org.hamcrest.Matchers.equalTo(3600));
        }

        @Test
        void authorizationAndCookieHeadersAreNotForwardedToAuthService() {
            auth.stubFor(post("/auth/login").willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json").withBody("{\"token\":\"t\"}")));

            given().contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + TestKeys.validToken())
                    .header("Cookie", "session=secret")
                    .body("{\"email\":\"a@b.com\",\"password\":\"x\"}")
                    .post("/auth/login").then().statusCode(200);

            auth.verify(postRequestedFor(urlEqualTo("/auth/login"))
                    .withHeader("Authorization", absent())
                    .withHeader("Cookie", absent()));
        }

        @Test
        void failedLoginIsRelayedAndNotRetried() {
            auth.stubFor(post("/auth/login").willReturn(aResponse().withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\":401,\"message\":\"invalid credentials\"}")));

            given().contentType(ContentType.JSON).body("{\"email\":\"a@b.com\",\"password\":\"wrong\"}")
                    .post("/auth/login")
                    .then().statusCode(401)
                    .body("message", org.hamcrest.Matchers.equalTo("invalid credentials"));

            auth.verify(1, postRequestedFor(urlEqualTo("/auth/login")));
        }
    }

    @Nested
    class JwtValidation {

        @Test
        void protectedRouteWithoutJwtIsRejectedAtTheGateway() {
            given().get("/api/expenses")
                    .then().statusCode(401)
                    .header("WWW-Authenticate", "Bearer")
                    .body("status", org.hamcrest.Matchers.equalTo(401))
                    .body("message", org.hamcrest.Matchers.equalTo("Missing, invalid or expired JWT"));

            expense.verify(0, anyRequestedFor(anyUrl()));
        }

        @Test
        void tokenSignedByUnknownKeyIsRejectedAtTheGateway() {
            given().auth().oauth2(TestKeys.forgedToken()).get("/api/expenses").then().statusCode(401);
            given().auth().oauth2(TestKeys.expiredToken()).get("/api/expenses").then().statusCode(401);
            given().auth().oauth2("garbage").contentType(ContentType.JSON).body("{}")
                    .post("/api/categories").then().statusCode(401);

            expense.verify(0, anyRequestedFor(anyUrl()));
        }
    }

    @Nested
    class Forwarding {

        @Test
        void validJwtIsForwardedWithOnlyRequiredHeaders() {
            String token = TestKeys.validToken();
            expense.stubFor(get("/api/expenses").willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json").withBody("[" + EXPENSE_JSON + "]")));

            given().auth().oauth2(token)
                    .header("Cookie", "session=secret")
                    .header("X-Internal-Admin", "true")
                    .get("/api/expenses")
                    .then().statusCode(200)
                    .body("[0].title", org.hamcrest.Matchers.equalTo("Weekly groceries"))
                    .body("[0].amount", org.hamcrest.Matchers.equalTo(82.45f));

            expense.verify(1, getRequestedFor(urlEqualTo("/api/expenses"))
                    .withHeader("Authorization", equalTo("Bearer " + token))
                    .withHeader("Cookie", absent())
                    .withHeader("X-Internal-Admin", absent()));
        }

        @Test
        void createIsForwardedAndLocationIsRewrittenToTheGateway() {
            expense.stubFor(post("/api/expenses").willReturn(aResponse().withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Location", "http://expense-service:8080/api/expenses/7")
                    .withBody(EXPENSE_JSON)));

            given().auth().oauth2(TestKeys.validToken()).contentType(ContentType.JSON)
                    .body("{\"title\":\"Weekly groceries\",\"amount\":82.45,\"categoryId\":3}")
                    .post("/api/expenses")
                    .then().statusCode(201)
                    .header("Location", endsWith("/api/expenses/7"))
                    .header("Location", not(org.hamcrest.Matchers.containsString("expense-service")))
                    .body("id", org.hamcrest.Matchers.equalTo(7));

            expense.verify(postRequestedFor(urlEqualTo("/api/expenses"))
                    .withRequestBody(equalToJson("{\"title\":\"Weekly groceries\",\"amount\":82.45,\"categoryId\":3}")));
        }

        @Test
        void downstreamValidationAndNotFoundErrorsAreRelayedUnchangedAndNotRetried() {
            expense.stubFor(put("/api/expenses/7").willReturn(aResponse().withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\":400,\"message\":\"Validation failed\",\"violations\":[{\"field\":\"amount\",\"message\":\"Expense amount must be greater than zero\"}]}")));
            expense.stubFor(get("/api/expenses/99").willReturn(aResponse().withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\":404,\"message\":\"Expense not found with ID: 99\"}")));

            given().auth().oauth2(TestKeys.validToken()).contentType(ContentType.JSON)
                    .body("{\"title\":\"x\",\"amount\":-1}")
                    .put("/api/expenses/7")
                    .then().statusCode(400)
                    .body("violations[0].field", org.hamcrest.Matchers.equalTo("amount"));
            given().auth().oauth2(TestKeys.validToken()).get("/api/expenses/99")
                    .then().statusCode(404)
                    .body("message", org.hamcrest.Matchers.equalTo("Expense not found with ID: 99"));

            expense.verify(1, putRequestedFor(urlEqualTo("/api/expenses/7")));
            expense.verify(1, getRequestedFor(urlEqualTo("/api/expenses/99")));
        }

        @Test
        void deleteAndCategoryRoutesAreForwarded() {
            expense.stubFor(delete("/api/expenses/7").willReturn(aResponse().withStatus(204)));
            expense.stubFor(get("/api/categories").willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{\"category_id\":3,\"categoryName\":\"Groceries\"}]")));

            given().auth().oauth2(TestKeys.validToken()).delete("/api/expenses/7").then().statusCode(204);
            given().auth().oauth2(TestKeys.validToken()).get("/api/categories")
                    .then().statusCode(200).body("[0].categoryName", org.hamcrest.Matchers.equalTo("Groceries"));

            expense.verify(1, deleteRequestedFor(urlEqualTo("/api/expenses/7")));
        }
    }

    @Nested
    class FaultTolerance {

        @Test
        void idempotentReadIsRetriedAfterConnectionFailureAndSucceeds() {
            expense.stubFor(get("/api/expenses/7").inScenario("flaky").whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)).willSetStateTo("recovered"));
            expense.stubFor(get("/api/expenses/7").inScenario("flaky").whenScenarioStateIs("recovered")
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(EXPENSE_JSON)));

            given().auth().oauth2(TestKeys.validToken()).get("/api/expenses/7")
                    .then().statusCode(200).body("title", org.hamcrest.Matchers.equalTo("Weekly groceries"));

            expense.verify(2, getRequestedFor(urlEqualTo("/api/expenses/7")));
        }

        @Test
        void slowReadTimesOutIsRetriedTwiceThenReturns504() {
            // test profile: Timeout/value=500ms, Retry/delay=50ms
            expense.stubFor(get("/api/expenses").willReturn(aResponse().withFixedDelay(1500)
                    .withStatus(200).withHeader("Content-Type", "application/json").withBody("[]")));

            given().auth().oauth2(TestKeys.validToken()).get("/api/expenses")
                    .then().statusCode(504)
                    .body("status", org.hamcrest.Matchers.equalTo(504))
                    .body("message", org.hamcrest.Matchers.equalTo("Downstream service did not respond in time"));

            expense.verify(3, getRequestedFor(urlEqualTo("/api/expenses"))); // 1 attempt + 2 retries
        }

        @Test
        void slowCreateTimesOutButIsNeverRetried() {
            expense.stubFor(post("/api/expenses").willReturn(aResponse().withFixedDelay(1500).withStatus(201)));

            given().auth().oauth2(TestKeys.validToken()).contentType(ContentType.JSON)
                    .body("{\"title\":\"Rent\",\"amount\":1200}")
                    .post("/api/expenses")
                    .then().statusCode(504);

            expense.verify(1, postRequestedFor(urlEqualTo("/api/expenses")));
        }

        @Test
        void createFailingAtTransportLevelIsNotRetried() {
            expense.stubFor(post("/api/expenses").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

            given().auth().oauth2(TestKeys.validToken()).contentType(ContentType.JSON)
                    .body("{\"title\":\"Rent\",\"amount\":1200}")
                    .post("/api/expenses")
                    .then().statusCode(503);

            expense.verify(1, postRequestedFor(urlEqualTo("/api/expenses")));
        }

        @Test
        void persistentConnectionFailureOnReadReturns503AfterRetries() {
            expense.stubFor(any(anyUrl()).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

            given().auth().oauth2(TestKeys.validToken()).get("/api/categories")
                    .then().statusCode(503)
                    .body("message", org.hamcrest.Matchers.equalTo("Downstream service is unavailable, please retry later"));

            expense.verify(3, getRequestedFor(urlEqualTo("/api/categories")));
        }
    }
}
