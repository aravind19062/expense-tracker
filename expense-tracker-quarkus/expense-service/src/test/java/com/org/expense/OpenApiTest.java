package com.org.expense;

import com.org.expense.support.JwtTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@WithTestResource(JwtTestResource.class)
class OpenApiTest {

    @Test
    void openApiDocumentDescribesExpenseEndpointsAndJwtScheme() {
        given().accept("application/json").get("/q/openapi").then().statusCode(200)
                .body("info.title", equalTo("Expense Service API"))
                .body("paths.'/api/expenses'.post", notNullValue())
                .body("paths.'/api/expenses/{id}'.delete", notNullValue())
                .body("paths.'/api/expenses/summary'.get", notNullValue())
                .body("paths.'/api/expenses/export/xml'.get", notNullValue())
                .body("components.securitySchemes.jwt.scheme", equalTo("bearer"));
    }

    @Test
    void swaggerUiIsServed() {
        given().get("/q/swagger-ui/").then().statusCode(200);
    }

    @Test
    void prometheusMetricsAreExposed() {
        given().get("/q/metrics").then().statusCode(200)
                .body(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes"));
    }

    @Test
    void healthIsUp() {
        given().get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP"));
    }
}
