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
            "{\"id\":7,\"title\":\"Weekly groceries\",\"amount\":82.45,\"category\":\"FOOD\",\"expenseDate\":\"2026-11-03\"}";

    @BeforeEach
    void resetMocks() {
        auth.resetAll();
        expense.resetAll();
    }

    @Nested
    class PublicAuthRoutes {

        @Test
        void registrationIsPublicAndRelaysDownstreamResponse() {
            auth.stubFor(post("/api/auth/register").willReturn(aResponse().withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":1,\"username\":\"priya\",\"email\":\"priya@example.com\"}")));

            given().contentType(ContentType.JSON)
                    .body("{\"username\":\"priya\",\"email\":\"priya@example.com\",\"password\":\"S3cure!pass\"}")
                    .post("/api/auth/register")
                    .then().statusCode(201)
                    .contentType(startsWith("application/json"))
                    .body("username", org.hamcrest.Matchers.equalTo("priya"));

            auth.verify(1, postRequestedFor(urlEqualTo("/api/auth/register"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withRequestBody(equalToJson("{\"username\":\"priya\",\"email\":\"priya@example.com\",\"password\":\"S3cure!pass\"}")));
        }

        @Test
        void loginIsPublicAndReturnsToken() {
            auth.stubFor(post("/api/auth/login").willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json").withBody("{\"token\":\"abc.def.ghi\",\"expiresIn\":3600}")));

            given().contentType(ContentType.JSON).body("{\"email\":\"priya@example.com\",\"password\":\"S3cure!pass\"}")
                    .post("/api/auth/login")
                    .then().statusCode(200)
                    .body("token", org.hamcrest.Matchers.equalTo("abc.def.ghi"))
                    .body("expiresIn", org.hamcrest.Matchers.equalTo(3600));
        }

        @Test
        void authorizationAndCookieHeadersAreNotForwardedToAuthService() {
            auth.stubFor(post("/api/auth/login").willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json").withBody("{\"token\":\"t\"}")));

            given().contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + TestKeys.validToken())
                    .header("Cookie", "session=secret")
                    .body("{\"email\":\"a@b.com\",\"password\":\"x\"}")
                    .post("/api/auth/login").then().statusCode(200);

            auth.verify(postRequestedFor(urlEqualTo("/api/auth/login"))
                    .withHeader("Authorization", absent())
                    .withHeader("Cookie", absent()));
        }

        @Test
        void failedLoginIsRelayedAndNotRetried() {
            auth.stubFor(post("/api/auth/login").willReturn(aResponse().withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"INVALID_CREDENTIALS\",\"message\":\"Invalid username or password\"}")));

            given().contentType(ContentType.JSON).body("{\"email\":\"a@b.com\",\"password\":\"wrong\"}")
                    .post("/api/auth/login")
                    .then().statusCode(401)
                    .body("code", org.hamcrest.Matchers.equalTo("INVALID_CREDENTIALS"));

            auth.verify(1, postRequestedFor(urlEqualTo("/api/auth/login")));
        }
    }

    @Nested
    class JwtValidation {

        @Test
        void protectedRouteWithoutJwtIsRejectedAtTheGateway() {
            given().get("/api/expenses")
                    .then().statusCode(401)
                    .header("WWW-Authenticate", "Bearer")
                    .body("code", org.hamcrest.Matchers.equalTo("UNAUTHORIZED"))
                    .body("message", org.hamcrest.Matchers.equalTo("Missing, invalid or expired JWT"))
                    .body("timestamp", org.hamcrest.Matchers.notNullValue());

            expense.verify(0, anyRequestedFor(anyUrl()));
        }

        @Test
        void tokenSignedByUnknownKeyIsRejectedAtTheGateway() {
            given().auth().oauth2(TestKeys.forgedToken()).get("/api/expenses").then().statusCode(401);
            given().auth().oauth2(TestKeys.expiredToken()).get("/api/expenses").then().statusCode(401);
            given().auth().oauth2("garbage").contentType(ContentType.JSON).body("{}")
                    .post("/api/expenses").then().statusCode(401);
            given().get("/api/expenses/export/xml").then().statusCode(401);

            expense.verify(0, anyRequestedFor(anyUrl()));
        }

        @Test
        void tokenWithoutUserGroupIsForbidden() {
            given().auth().oauth2(TestKeys.tokenWithGroups("guest")).get("/api/expenses")
                    .then().statusCode(403)
                    .body("code", org.hamcrest.Matchers.equalTo("FORBIDDEN"));

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
                    .withHeader("Location", "http://expense-service:8082/api/expenses/7")
                    .withBody(EXPENSE_JSON)));

            given().auth().oauth2(TestKeys.validToken()).contentType(ContentType.JSON)
                    .body("{\"title\":\"Weekly groceries\",\"amount\":82.45,\"category\":\"FOOD\",\"expenseDate\":\"2026-11-03\"}")
                    .post("/api/expenses")
                    .then().statusCode(201)
                    .header("Location", endsWith("/api/expenses/7"))
                    .header("Location", not(org.hamcrest.Matchers.containsString("expense-service")))
                    .body("id", org.hamcrest.Matchers.equalTo(7));

            expense.verify(postRequestedFor(urlEqualTo("/api/expenses"))
                    .withRequestBody(equalToJson("{\"title\":\"Weekly groceries\",\"amount\":82.45,\"category\":\"FOOD\",\"expenseDate\":\"2026-11-03\"}")));
        }

        @Test
        void downstreamValidationAndNotFoundErrorsAreRelayedUnchangedAndNotRetried() {
            expense.stubFor(put("/api/expenses/7").willReturn(aResponse().withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"VALIDATION_FAILED\",\"message\":\"Validation failed\",\"details\":[{\"field\":\"amount\",\"message\":\"Amount must be greater than zero\"}]}")));
            expense.stubFor(get("/api/expenses/99").willReturn(aResponse().withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"EXPENSE_NOT_FOUND\",\"message\":\"Expense 99 not found\"}")));

            given().auth().oauth2(TestKeys.validToken()).contentType(ContentType.JSON)
                    .body("{\"title\":\"x\",\"amount\":-1}")
                    .put("/api/expenses/7")
                    .then().statusCode(400)
                    .body("details[0].field", org.hamcrest.Matchers.equalTo("amount"));
            given().auth().oauth2(TestKeys.validToken()).get("/api/expenses/99")
                    .then().statusCode(404)
                    .body("code", org.hamcrest.Matchers.equalTo("EXPENSE_NOT_FOUND"));

            expense.verify(1, putRequestedFor(urlEqualTo("/api/expenses/7")));
            expense.verify(1, getRequestedFor(urlEqualTo("/api/expenses/99")));
        }

        @Test
        void pagingAndCategoryQueryParametersAreForwarded() {
            expense.stubFor(get(urlEqualTo("/api/expenses?page=1&size=5&category=FOOD")).willReturn(aResponse()
                    .withStatus(200).withHeader("Content-Type", "application/json").withBody("[" + EXPENSE_JSON + "]")));

            given().auth().oauth2(TestKeys.validToken())
                    .queryParam("page", 1).queryParam("size", 5).queryParam("category", "FOOD")
                    .get("/api/expenses")
                    .then().statusCode(200).body("[0].category", org.hamcrest.Matchers.equalTo("FOOD"));

            expense.verify(1, getRequestedFor(urlEqualTo("/api/expenses?page=1&size=5&category=FOOD")));
        }

        @Test
        void deleteSummaryAndXmlExportAreForwarded() {
            expense.stubFor(delete("/api/expenses/7").willReturn(aResponse().withStatus(204)));
            expense.stubFor(get("/api/expenses/summary").willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"FOOD\":82.45,\"TRAVEL\":0.00,\"BILLS\":0.00,\"SHOPPING\":0.00,\"OTHER\":0.00}")));
            expense.stubFor(get("/api/expenses/export/xml").willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/xml")
                    .withBody("<Expenses><Expense><Description>WEEKLY GROCERIES</Description><Amt>82.45</Amt>"
                            + "<CatCode>F01</CatCode><TxnDate>03-11-2026</TxnDate></Expense></Expenses>")));

            given().auth().oauth2(TestKeys.validToken()).delete("/api/expenses/7").then().statusCode(204);
            given().auth().oauth2(TestKeys.validToken()).get("/api/expenses/summary")
                    .then().statusCode(200).body("FOOD", org.hamcrest.Matchers.equalTo(82.45f));
            given().auth().oauth2(TestKeys.validToken()).get("/api/expenses/export/xml")
                    .then().statusCode(200)
                    .contentType(startsWith("application/xml"))
                    .body("Expenses.Expense.CatCode", org.hamcrest.Matchers.equalTo("F01"));

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
            // test profile overrides @Timeout(2000) with 500ms and Retry/delay with 50ms to keep the test fast
            expense.stubFor(get("/api/expenses").willReturn(aResponse().withFixedDelay(1500)
                    .withStatus(200).withHeader("Content-Type", "application/json").withBody("[]")));

            given().auth().oauth2(TestKeys.validToken()).get("/api/expenses")
                    .then().statusCode(504)
                    .body("code", org.hamcrest.Matchers.equalTo("GATEWAY_TIMEOUT"))
                    .body("message", org.hamcrest.Matchers.equalTo("Downstream service did not respond in time"));

            expense.verify(3, getRequestedFor(urlEqualTo("/api/expenses"))); // 1 attempt + 2 retries
        }

        @Test
        void slowCreateTimesOutButIsNeverRetried() {
            expense.stubFor(post("/api/expenses").willReturn(aResponse().withFixedDelay(1500).withStatus(201)));

            given().auth().oauth2(TestKeys.validToken()).contentType(ContentType.JSON)
                    .body("{\"title\":\"Rent\",\"amount\":1200,\"category\":\"BILLS\",\"expenseDate\":\"2026-11-01\"}")
                    .post("/api/expenses")
                    .then().statusCode(504);

            expense.verify(1, postRequestedFor(urlEqualTo("/api/expenses")));
        }

        @Test
        void createFailingAtTransportLevelIsNotRetried() {
            expense.stubFor(post("/api/expenses").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

            given().auth().oauth2(TestKeys.validToken()).contentType(ContentType.JSON)
                    .body("{\"title\":\"Rent\",\"amount\":1200,\"category\":\"BILLS\",\"expenseDate\":\"2026-11-01\"}")
                    .post("/api/expenses")
                    .then().statusCode(503);

            expense.verify(1, postRequestedFor(urlEqualTo("/api/expenses")));
        }

        @Test
        void persistentConnectionFailureOnReadReturns503AfterRetries() {
            expense.stubFor(any(anyUrl()).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

            given().auth().oauth2(TestKeys.validToken()).get("/api/expenses/summary")
                    .then().statusCode(503)
                    .body("code", org.hamcrest.Matchers.equalTo("SERVICE_UNAVAILABLE"));

            expense.verify(3, getRequestedFor(urlEqualTo("/api/expenses/summary")));
        }
    }
}
