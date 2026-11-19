package com.org.expense;

import com.org.expense.support.DbCleaner;
import com.org.expense.support.JwtTestResource;
import com.org.expense.support.Tokens;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/** Ports the Spring CategoryControllerIntegrationTest cases to RestAssured + JWT. */
@QuarkusTest
@WithTestResource(JwtTestResource.class)
class CategoryResourceTest {

    @Inject
    DbCleaner dbCleaner;

    @BeforeEach
    void cleanDatabase() {
        dbCleaner.clean();
    }

    private static RequestSpecification user() {
        return given().auth().oauth2(Tokens.validFor("101")).contentType(ContentType.JSON);
    }

    private static long create(String name) {
        return user().body(Map.of("categoryName", name)).post("/api/categories")
                .then().statusCode(201).extract().jsonPath().getLong("category_id");
    }

    @Test
    void createCategoryKeepsOriginalJsonShape() {
        user().body(Map.of("categoryName", "Essentials")).post("/api/categories")
                .then().statusCode(201)
                .header("Location", notNullValue())
                .body("category_id", notNullValue())
                .body("categoryName", equalTo("Essentials"));
    }

    @Test
    void getById() {
        long id = create("Essentials");
        user().get("/api/categories/{id}", id).then().statusCode(200)
                .body("category_id", equalTo((int) id))
                .body("categoryName", equalTo("Essentials"));
    }

    @Test
    void getAll() {
        create("Essentials");
        create("Fun");
        user().get("/api/categories").then().statusCode(200)
                .body("categoryName", containsInAnyOrder("Essentials", "Fun"));
    }

    @Test
    void update() {
        long id = create("Essentials");
        user().body(Map.of("categoryName", "All Essentials")).put("/api/categories/{id}", id)
                .then().statusCode(200)
                .body("category_id", equalTo((int) id))
                .body("categoryName", equalTo("All Essentials"));
    }

    @Test
    void deleteUnusedCategory() {
        long id = create("Essentials");
        user().delete("/api/categories/{id}", id).then().statusCode(204);
        user().get("/api/categories/{id}", id).then().statusCode(404);
    }

    @Test
    void deleteCategoryInUseReturns409AndKeepsExpense() {
        long id = create("Rent");
        long expenseId = user().body(Map.of("title", "November rent", "amount", 1200, "categoryId", id))
                .post("/api/expenses").then().statusCode(201).extract().jsonPath().getLong("id");

        user().delete("/api/categories/{id}", id).then().statusCode(409)
                .body("message", equalTo("Category " + id + " is still used by one or more expenses"));
        user().get("/api/expenses/{id}", expenseId).then().statusCode(200);
    }

    @Test
    void blankNameReturns400() {
        user().body(Map.of("categoryName", "")).post("/api/categories")
                .then().statusCode(400).body("violations[0].field", equalTo("categoryName"));
    }

    @Test
    void unknownCategoryReturns404() {
        user().get("/api/categories/{id}", 777).then().statusCode(404)
                .body("message", equalTo("Category not found with ID: 777"));
    }

    @Test
    void requiresJwt() {
        given().get("/api/categories").then().statusCode(401);
    }
}
