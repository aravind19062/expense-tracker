package com.org.expense;

import com.org.expense.support.DbCleaner;
import com.org.expense.support.JwtTestResource;
import com.org.expense.support.Tokens;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@WithTestResource(JwtTestResource.class)
class ExpenseResourceTest {

    private static final String ALICE = "101";
    private static final String BOB = "202";

    @Inject
    DbCleaner dbCleaner;

    @BeforeEach
    void cleanDatabase() {
        dbCleaner.clean();
    }

    private static io.restassured.specification.RequestSpecification as(String userId) {
        return given().auth().oauth2(Tokens.validFor(userId)).contentType(ContentType.JSON);
    }

    private static long createCategory(String userId, String name) {
        return as(userId).body(Map.of("categoryName", name))
                .post("/api/categories").then().statusCode(201)
                .extract().jsonPath().getLong("category_id");
    }

    private static long createExpense(String userId, String title, String amount, Long categoryId) {
        var body = new java.util.HashMap<String, Object>();
        body.put("title", title);
        body.put("amount", new java.math.BigDecimal(amount));
        body.put("categoryId", categoryId);
        return as(userId).body(body).post("/api/expenses")
                .then().statusCode(201).extract().jsonPath().getLong("id");
    }

    @Test
    void createExpenseReturns201WithLocationAndBody() {
        long groceries = createCategory(ALICE, "Groceries");

        as(ALICE).body(Map.of("title", "Weekly groceries", "amount", 82.45, "categoryId", groceries))
                .when().post("/api/expenses")
                .then().statusCode(201)
                .header("Location", notNullValue())
                .body("id", notNullValue())
                .body("title", equalTo("Weekly groceries"))
                .body("amount", equalTo(82.45f))
                .body("categoryId", equalTo((int) groceries))
                .body("categoryName", equalTo("Groceries"));
    }

    @Test
    void createdExpenseIsRetrievableViaLocationHeader() {
        String location = as(ALICE).body(Map.of("title", "Train ticket", "amount", 12.50))
                .post("/api/expenses").then().statusCode(201).extract().header("Location");

        as(ALICE).get(location).then().statusCode(200)
                .body("title", equalTo("Train ticket"))
                .body("categoryId", nullValue());
    }

    @Test
    void fetchAllReturnsOnlyTheCallersExpensesInCreationOrder() {
        createExpense(ALICE, "Rent", "1200.00", null);
        createExpense(ALICE, "Electricity bill", "64.30", null);
        createExpense(BOB, "Gym membership", "40.00", null);

        as(ALICE).get("/api/expenses").then().statusCode(200)
                .body("$", hasSize(2))
                .body("title", contains("Rent", "Electricity bill"))
                .body("amount", contains(1200.00f, 64.30f));

        as(BOB).get("/api/expenses").then().statusCode(200)
                .body("title", contains("Gym membership"));
    }

    @Test
    void fetchByIdReturnsExpense() {
        long travel = createCategory(ALICE, "Travel");
        long id = createExpense(ALICE, "Flight to Bangalore", "5499.99", travel);

        as(ALICE).get("/api/expenses/{id}", id).then().statusCode(200)
                .body("id", equalTo((int) id))
                .body("title", equalTo("Flight to Bangalore"))
                .body("amount", equalTo(5499.99f))
                .body("categoryName", equalTo("Travel"));
    }

    @Test
    void updateReplacesTitleAmountAndCategory() {
        long food = createCategory(ALICE, "Food");
        long dining = createCategory(ALICE, "Dining out");
        long id = createExpense(ALICE, "Lunch", "15.00", food);

        as(ALICE).body(Map.of("title", "Team lunch", "amount", 48.75, "categoryId", dining))
                .put("/api/expenses/{id}", id)
                .then().statusCode(200)
                .body("id", equalTo((int) id))
                .body("title", equalTo("Team lunch"))
                .body("amount", equalTo(48.75f))
                .body("categoryName", equalTo("Dining out"));

        as(ALICE).get("/api/expenses/{id}", id).then().body("title", equalTo("Team lunch"));
    }

    @Test
    void deleteReturns204AndRemovesExpense() {
        long id = createExpense(ALICE, "Movie tickets", "22.00", null);

        as(ALICE).delete("/api/expenses/{id}", id).then().statusCode(204);
        as(ALICE).get("/api/expenses/{id}", id).then().statusCode(404);
        as(ALICE).get("/api/expenses").then().body("$", hasSize(0));
    }

    @Nested
    class Validation {

        @Test
        void negativeAmountAndBlankTitleReturn400WithFieldErrors() {
            as(ALICE).body(Map.of("title", "  ", "amount", -5))
                    .post("/api/expenses")
                    .then().statusCode(400)
                    .body("status", equalTo(400))
                    .body("message", equalTo("Validation failed"))
                    .body("violations.field", containsInAnyOrder("amount", "title"))
                    .body("violations.find { it.field == 'amount' }.message",
                            equalTo("Expense amount must be greater than zero"));
        }

        @Test
        void zeroAmountIsRejectedLikeTheOriginalService() {
            as(ALICE).body(Map.of("title", "Free sample", "amount", 0))
                    .post("/api/expenses")
                    .then().statusCode(400)
                    .body("violations[0].field", equalTo("amount"));
        }

        @Test
        void missingAmountReturns400() {
            as(ALICE).body(Map.of("title", "No amount"))
                    .post("/api/expenses")
                    .then().statusCode(400)
                    .body("violations[0].field", equalTo("amount"))
                    .body("violations[0].message", equalTo("Expense amount is required"));
        }

        @Test
        void titleLongerThan100CharsReturns400() {
            as(ALICE).body(Map.of("title", "x".repeat(101), "amount", 10))
                    .post("/api/expenses")
                    .then().statusCode(400)
                    .body("violations[0].field", equalTo("title"));
        }

        @Test
        void malformedJsonReturns400() {
            as(ALICE).body("{\"title\": \"oops\", \"amount\": ")
                    .post("/api/expenses")
                    .then().statusCode(400)
                    .body("status", equalTo(400));
        }

        @Test
        void invalidUpdateReturns400AndLeavesExpenseUnchanged() {
            long id = createExpense(ALICE, "Coffee", "3.50", null);

            as(ALICE).body(Map.of("title", "Coffee", "amount", -1))
                    .put("/api/expenses/{id}", id).then().statusCode(400);

            as(ALICE).get("/api/expenses/{id}", id).then().body("amount", equalTo(3.50f));
        }
    }

    @Nested
    class NotFound {

        @Test
        void unknownIdReturns404WithMessage() {
            as(ALICE).get("/api/expenses/{id}", 999_999).then().statusCode(404)
                    .body("status", equalTo(404))
                    .body("message", equalTo("Expense not found with ID: 999999"))
                    .body("path", endsWith("/api/expenses/999999"));
        }

        @Test
        void updatingUnknownIdReturns404() {
            as(ALICE).body(Map.of("title", "Ghost", "amount", 1))
                    .put("/api/expenses/{id}", 999_999).then().statusCode(404);
        }

        @Test
        void deletingUnknownIdReturns404() {
            as(ALICE).delete("/api/expenses/{id}", 999_999).then().statusCode(404);
        }

        @Test
        void unknownCategoryOnCreateReturns404() {
            as(ALICE).body(Map.of("title", "Books", "amount", 30, "categoryId", 424242))
                    .post("/api/expenses").then().statusCode(404)
                    .body("message", equalTo("Category not found with ID: 424242"));
        }

        @Test
        void anotherUsersExpenseIsInvisible() {
            long id = createExpense(ALICE, "Private expense", "99.00", null);

            as(BOB).get("/api/expenses/{id}", id).then().statusCode(404);
            as(BOB).body(Map.of("title", "Hijack", "amount", 1)).put("/api/expenses/{id}", id).then().statusCode(404);
            as(BOB).delete("/api/expenses/{id}", id).then().statusCode(404);

            as(ALICE).get("/api/expenses/{id}", id).then().statusCode(200).body("title", equalTo("Private expense"));
        }
    }

    @Nested
    class Security {

        @Test
        void requestWithoutJwtReturns401() {
            given().get("/api/expenses").then().statusCode(401);
            given().contentType(ContentType.JSON).body(Map.of("title", "x", "amount", 1))
                    .post("/api/expenses").then().statusCode(401);
        }

        @Test
        void requestWithValidJwtSucceeds() {
            given().auth().oauth2(Tokens.validFor(ALICE)).get("/api/expenses").then().statusCode(200);
        }

        @Test
        void tokenSignedWithUntrustedKeyIsRejected() {
            given().auth().oauth2(Tokens.signedWithUntrustedKey(ALICE)).get("/api/expenses").then().statusCode(401);
        }

        @Test
        void tamperedTokenIsRejected() {
            String[] parts = Tokens.validFor(ALICE).split("\\.");
            // swap in the payload of a token for a different user while keeping Alice's signature
            String[] bobParts = Tokens.validFor(BOB).split("\\.");
            String tampered = parts[0] + "." + bobParts[1] + "." + parts[2];
            given().auth().oauth2(tampered).get("/api/expenses").then().statusCode(401);
        }

        @Test
        void expiredTokenIsRejected() {
            given().auth().oauth2(Tokens.expiredFor(ALICE)).get("/api/expenses").then().statusCode(401);
        }

        @Test
        void tokenFromWrongIssuerIsRejected() {
            given().auth().oauth2(Tokens.wrongIssuer(ALICE)).get("/api/expenses").then().statusCode(401);
        }

        @Test
        void garbageTokenIsRejected() {
            given().auth().oauth2("not-a-jwt").get("/api/expenses").then().statusCode(401);
        }

        @Test
        void validTokenWithoutUserRoleReturns403() {
            given().auth().oauth2(Tokens.withoutUserRole(ALICE)).get("/api/expenses").then().statusCode(403);
        }
    }
}
