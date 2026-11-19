package com.org.auth;

import com.org.auth.entity.Role;
import com.org.auth.entity.User;
import com.org.auth.support.TestSigningKey;
import com.org.auth.support.TestUsers;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@WithTestResource(TestSigningKey.class)
class AuthResourceTest {

    private static final String ISSUER = "https://expense-tracker.local/auth";
    private static final String EMAIL = "priya.sharma@example.com";
    private static final String PASSWORD = "S3cure!pass";

    @Inject
    TestUsers testUsers;

    @BeforeEach
    void cleanUsers() {
        testUsers.deleteAll();
    }

    private static void register(String userName, String email, String password) {
        given().contentType(ContentType.JSON)
                .body(Map.of("userName", userName, "email", email, "password", password))
                .post("/auth/register").then().statusCode(201);
    }

    private static String login(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/auth/login").then().statusCode(200).extract().path("token");
    }

    /** Verifies a token exactly the way a downstream service would: signature checked against the published JWKS. */
    private static JwtClaims verify(String token, NumericDate evaluationTime) throws Exception {
        String jwks = given().get("/auth/jwks").then().statusCode(200).extract().asString();
        JwtConsumerBuilder builder = new JwtConsumerBuilder()
                .setVerificationKeyResolver(new JwksVerificationKeyResolver(new JsonWebKeySet(jwks).getJsonWebKeys()))
                .setExpectedIssuer(ISSUER)
                .setRequireExpirationTime()
                .setRequireIssuedAt()
                .setRequireSubject()
                .setSkipDefaultAudienceValidation();
        if (evaluationTime != null) {
            builder.setEvaluationTime(evaluationTime);
        }
        JwtConsumer consumer = builder.build();
        return consumer.processToClaims(token);
    }

    @Nested
    class Registration {

        @Test
        void registerReturns201WithOriginalMessageAndStoresUserWithUserRole() {
            given().contentType(ContentType.JSON)
                    .body(Map.of("userName", "Priya Sharma", "email", "Priya.Sharma@Example.com", "password", PASSWORD))
                    .post("/auth/register")
                    .then().statusCode(201)
                    .contentType(ContentType.TEXT)
                    .body(equalTo("User registered successfully"));

            User stored = testUsers.find(EMAIL); // email normalized to lower case
            assertEquals("Priya Sharma", stored.userName);
            assertEquals(Role.USER, stored.role);
        }

        @Test
        void passwordIsStoredAsSaltedBcryptHash() {
            register("Priya", EMAIL, PASSWORD);
            register("Rahul", "rahul.verma@example.com", PASSWORD);

            String hash = testUsers.find(EMAIL).password;
            assertNotEquals(PASSWORD, hash);
            assertTrue(hash.startsWith("$2a$10$"), "expected BCrypt MCF hash with cost 10 but was " + hash);
            assertTrue(BcryptUtil.matches(PASSWORD, hash));
            assertFalse(BcryptUtil.matches("wrong-password", hash));
            // same password, different salt => different hash
            assertNotEquals(hash, testUsers.find("rahul.verma@example.com").password);
        }

        @Test
        void duplicateEmailIsRejectedCaseInsensitively() {
            register("Priya", EMAIL, PASSWORD);

            given().contentType(ContentType.JSON)
                    .body(Map.of("userName", "Impostor", "email", "PRIYA.SHARMA@example.com", "password", "another1"))
                    .post("/auth/register")
                    .then().statusCode(400)
                    .body("message", equalTo("User already exists"));
            assertEquals(1, testUsers.count());
        }

        @Test
        void invalidRegistrationReturns400WithFieldErrors() {
            given().contentType(ContentType.JSON)
                    .body(Map.of("userName", "", "email", "not-an-email", "password", "123"))
                    .post("/auth/register")
                    .then().statusCode(400)
                    .body("violations.field", containsInAnyOrder("email", "password", "userName"));
            assertEquals(0, testUsers.count());
        }

        @Test
        void clientCannotSelfAssignAdminRole() {
            given().contentType(ContentType.JSON)
                    .body(Map.of("userName", "Mallory", "email", "mallory@example.com", "password", PASSWORD, "role", "ADMIN"))
                    .post("/auth/register")
                    .then().statusCode(201);
            assertEquals(Role.USER, testUsers.find("mallory@example.com").role);
        }
    }

    @Nested
    class Login {

        @Test
        void successfulLoginReturnsTokenAndLifetime() {
            register("Priya", EMAIL, PASSWORD);

            given().contentType(ContentType.JSON)
                    .body(Map.of("email", EMAIL, "password", PASSWORD))
                    .post("/auth/login")
                    .then().statusCode(200)
                    .body("token", org.hamcrest.Matchers.matchesPattern("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$"))
                    .body("expiresIn", equalTo(3600));
        }

        @Test
        void wrongPasswordReturns401() {
            register("Priya", EMAIL, PASSWORD);

            given().contentType(ContentType.JSON)
                    .body(Map.of("email", EMAIL, "password", "WrongPass1"))
                    .post("/auth/login")
                    .then().statusCode(401)
                    .body("message", equalTo("invalid credentials"))
                    .body("$", not(org.hamcrest.Matchers.hasKey("token")));
        }

        @Test
        void unknownEmailReturnsSame401AsWrongPassword() {
            given().contentType(ContentType.JSON)
                    .body(Map.of("email", "nobody@example.com", "password", PASSWORD))
                    .post("/auth/login")
                    .then().statusCode(401)
                    .body("message", equalTo("invalid credentials"));
        }

        @Test
        void missingFieldsReturn400() {
            given().contentType(ContentType.JSON).body(Map.of("email", EMAIL))
                    .post("/auth/login")
                    .then().statusCode(400)
                    .body("violations.field", hasItem("password"));
        }
    }

    @Nested
    class Tokens {

        @Test
        void tokenIsRs256SignedAndCarriesExpectedClaims() throws Exception {
            register("Priya Sharma", EMAIL, PASSWORD);
            User user = testUsers.find(EMAIL);

            String token = login(EMAIL, PASSWORD);
            JwtClaims claims = verify(token, null);

            assertEquals(ISSUER, claims.getIssuer());
            assertEquals(String.valueOf(user.id), claims.getSubject());
            assertEquals(EMAIL, claims.getStringClaimValue("upn"));
            assertEquals(EMAIL, claims.getStringClaimValue("email"));
            assertEquals("Priya Sharma", claims.getStringClaimValue("preferred_username"));
            assertEquals(java.util.List.of("USER"), claims.getStringListClaimValue("groups"));

            String header = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]));
            assertTrue(header.contains("\"alg\":\"RS256\""), header);
            assertTrue(header.contains("\"kid\""), header);
        }

        @Test
        void tokenExpiresAfterConfiguredLifespan() throws Exception {
            register("Priya", EMAIL, PASSWORD);
            String token = login(EMAIL, PASSWORD);

            JwtClaims claims = verify(token, null);
            long iat = claims.getIssuedAt().getValue();
            long exp = claims.getExpirationTime().getValue();
            assertEquals(3600, exp - iat);

            // A verifier evaluating the token after its expiry (plus jose4j's default leeway) must reject it.
            NumericDate afterExpiry = NumericDate.fromSeconds(exp + 120);
            InvalidJwtException e = assertThrows(InvalidJwtException.class, () -> verify(token, afterExpiry));
            assertTrue(e.hasExpired());
        }

        @Test
        void adminRoleIsReflectedInGroupsClaim() throws Exception {
            register("Admin", "admin@example.com", PASSWORD);
            testUsers.setRole("admin@example.com", Role.ADMIN);

            JwtClaims claims = verify(login("admin@example.com", PASSWORD), null);
            assertEquals(java.util.Set.of("ADMIN", "USER"), java.util.Set.copyOf(claims.getStringListClaimValue("groups")));
        }

        @Test
        void tamperedTokenFailsSignatureVerification() {
            register("Priya", EMAIL, PASSWORD);
            String[] parts = login(EMAIL, PASSWORD).split("\\.");
            String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                    new String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                            .replace("[\"USER\"]", "[\"ADMIN\"]").getBytes());
            String forged = parts[0] + "." + forgedPayload + "." + parts[2];

            assertThrows(InvalidJwtException.class, () -> verify(forged, null));
        }

        @Test
        void jwksExposesOnlyPublicKeyMaterial() {
            given().get("/auth/jwks").then().statusCode(200)
                    .body("keys", org.hamcrest.Matchers.hasSize(1))
                    .body("keys[0].kty", equalTo("RSA"))
                    .body("keys[0].alg", equalTo("RS256"))
                    .body("keys[0].use", equalTo("sig"))
                    .body("keys[0]", not(org.hamcrest.Matchers.hasKey("d")))
                    .body("keys[0]", not(org.hamcrest.Matchers.hasKey("p")));
        }
    }
}
