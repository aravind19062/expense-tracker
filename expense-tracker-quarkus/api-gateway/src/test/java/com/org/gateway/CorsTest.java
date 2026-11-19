package com.org.gateway;

import com.org.gateway.support.DownstreamMocks;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@WithTestResource(DownstreamMocks.class)
class CorsTest {

    @Test
    void preflightFromAngularDevServerIsAllowed() {
        given().header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .options("/api/expenses")
                .then().statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:4200"))
                .header("Access-Control-Allow-Headers", containsString("Authorization"));
    }

    @Test
    void unknownOriginIsNotAllowed() {
        given().header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", "GET")
                .options("/api/expenses")
                .then().header("Access-Control-Allow-Origin", nullValue());
    }
}
