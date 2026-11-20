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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@WithTestResource(JwtTestResource.class)
class ExpenseResourceTest {

    private static final String ALICE = "101";
    private static final String BOB = "202";
    private static final String TODAY = LocalDate.now().toString();

    @Inject
    DbCleaner dbCleaner;

    @BeforeEach
    void cleanDatabase() {
        dbCleaner.clean();
    }

    private static RequestSpecification as(String userId) {
        return given().auth().oauth2(Tokens.validFor(userId)).contentType(ContentType.JSON);
    }

    private static Map<String, Object> body(String title, Object amount, String category, String date) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("amount", amount);
        body.put("category", category);
        body.put("expenseDate", date);
        return body;
    }

    private static long create(String userId, String title, String amount, String category, String date) {
        return as(userId).body(body(title, new java.math.BigDecimal(amount), category, date))
                .post("/api/expenses").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    @Nested
    class Create {

        @Test
        void returns201WithLocationAndCreatedExpense() {
            as(ALICE).body(body("Weekly groceries", 82.45, "FOOD", "2026-11-03"))
                    .post("/api/expenses")
                    .then().statusCode(201)
                    .header("Location", containsString("/api/expenses/"))
                    .body("id", notNullValue())
                    .body("userId", equalTo(ALICE))
                    .body("title", equalTo("Weekly groceries"))
                    .body("amount", equalTo(82.45f))
                    .body("category", equalTo("FOOD"))
                    .body("expenseDate", equalTo("2026-11-03"))
                    .body("createdAt", notNullValue());
        }

        @Test
        void createdExpenseIsRetrievableViaLocationHeader() {
            String location = as(ALICE).body(body("Train ticket", 12.50, "TRAVEL", TODAY))
                    .post("/api/expenses").then().statusCode(201).extract().header("Location");

            as(ALICE).get(location).then().statusCode(200)
                    .body("title", equalTo("Train ticket"))
                    .body("category", equalTo("TRAVEL"))
                    .body("expenseDate", equalTo(TODAY));
        }

        @Test
        void todayIsAllowed() {
            as(ALICE).body(body("Coffee", 3.5, "FOOD", TODAY)).post("/api/expenses").then().statusCode(201);
        }
    }

    @Nested
    class Read {

        @Test
        void getByIdReturns200() {
            long id = create(ALICE, "Flight to Bangalore", "5499.99", "TRAVEL", "2026-10-15");

            as(ALICE).get("/api/expenses/{id}", id).then().statusCode(200)
                    .body("id", equalTo((int) id))
                    .body("amount", equalTo(5499.99f))
                    .body("category", equalTo("TRAVEL"));
        }

        @Test
        void listReturnsOnlyCallersExpensesNewestFirst() {
            create(ALICE, "Rent", "1200.00", "BILLS", "2026-11-01");
            create(ALICE, "Dinner", "40.00", "FOOD", "2026-11-05");
            create(BOB, "Gym", "40.00", "OTHER", "2026-11-02");

            as(ALICE).get("/api/expenses").then().statusCode(200)
                    .header("X-Total-Count", "2")
                    .body("title", contains("Dinner", "Rent"));
            as(BOB).get("/api/expenses").then().body("title", contains("Gym"));
        }

        @Test
        void listIsPaged() {
            for (int day = 1; day <= 12; day++) {
                create(ALICE, "Day " + day, "1.00", "FOOD", String.format("2026-10-%02d", day));
            }

            as(ALICE).get("/api/expenses").then().statusCode(200)
                    .header("X-Total-Count", "12")
                    .body("$", hasSize(10)); // default size=10
            as(ALICE).queryParam("page", 0).queryParam("size", 5).get("/api/expenses").then()
                    .body("title", contains("Day 12", "Day 11", "Day 10", "Day 9", "Day 8"));
            as(ALICE).queryParam("page", 2).queryParam("size", 5).get("/api/expenses").then()
                    .body("title", contains("Day 2", "Day 1"));
            as(ALICE).queryParam("page", 3).queryParam("size", 5).get("/api/expenses").then()
                    .statusCode(200).body("$", hasSize(0));
        }

        @Test
        void listFiltersByCategory() {
            create(ALICE, "Lunch", "12.00", "FOOD", "2026-11-01");
            create(ALICE, "Taxi", "30.00", "TRAVEL", "2026-11-02");
            create(ALICE, "Dinner", "25.00", "FOOD", "2026-11-03");

            as(ALICE).queryParam("category", "FOOD").get("/api/expenses").then().statusCode(200)
                    .header("X-Total-Count", "2")
                    .body("title", contains("Dinner", "Lunch"))
                    .body("category", contains("FOOD", "FOOD"));
        }

        @Test
        void invalidPagingOrCategoryReturns400() {
            as(ALICE).queryParam("size", 0).get("/api/expenses").then().statusCode(400)
                    .body("code", equalTo("VALIDATION_FAILED"));
            as(ALICE).queryParam("size", 101).get("/api/expenses").then().statusCode(400);
            as(ALICE).queryParam("page", -1).get("/api/expenses").then().statusCode(400);
            as(ALICE).queryParam("category", "PETS").get("/api/expenses").then().statusCode(400)
                    .body("details[0].field", equalTo("category"));
        }
    }

    @Nested
    class Update {

        @Test
        void putReplacesAllFields() {
            long id = create(ALICE, "Lunch", "15.00", "FOOD", "2026-11-01");

            as(ALICE).body(body("Team offsite travel", 480.75, "TRAVEL", "2026-11-02"))
                    .put("/api/expenses/{id}", id)
                    .then().statusCode(200)
                    .body("id", equalTo((int) id))
                    .body("title", equalTo("Team offsite travel"))
                    .body("amount", equalTo(480.75f))
                    .body("category", equalTo("TRAVEL"))
                    .body("expenseDate", equalTo("2026-11-02"));

            as(ALICE).get("/api/expenses/{id}", id).then().body("title", equalTo("Team offsite travel"));
        }

        @Test
        void invalidBodyReturns400AndLeavesExpenseUnchanged() {
            long id = create(ALICE, "Coffee", "3.50", "FOOD", "2026-11-01");

            as(ALICE).body(body("Coffee", -1, "FOOD", "2026-11-01"))
                    .put("/api/expenses/{id}", id).then().statusCode(400);

            as(ALICE).get("/api/expenses/{id}", id).then().body("amount", equalTo(3.50f));
        }

        @Test
        void unknownIdReturns404() {
            as(ALICE).body(body("Ghost", 1, "OTHER", TODAY)).put("/api/expenses/{id}", 999_999)
                    .then().statusCode(404).body("code", equalTo("EXPENSE_NOT_FOUND"));
        }
    }

    @Nested
    class Delete {

        @Test
        void returns204AndRemovesExpense() {
            long id = create(ALICE, "Movie tickets", "22.00", "OTHER", "2026-11-01");

            as(ALICE).delete("/api/expenses/{id}", id).then().statusCode(204);
            as(ALICE).get("/api/expenses/{id}", id).then().statusCode(404);
        }

        @Test
        void unknownIdReturns404() {
            as(ALICE).delete("/api/expenses/{id}", 999_999).then().statusCode(404)
                    .body("code", equalTo("EXPENSE_NOT_FOUND"));
        }
    }

    @Nested
    class Summary {

        @Test
        void totalsPerCategoryIncludingZeroes() {
            create(ALICE, "Lunch", "12.50", "FOOD", "2026-11-01");
            create(ALICE, "Dinner", "30.00", "FOOD", "2026-11-02");
            create(ALICE, "Electricity", "64.30", "BILLS", "2026-11-03");
            create(BOB, "Flight", "999.00", "TRAVEL", "2026-11-03");

            as(ALICE).get("/api/expenses/summary").then().statusCode(200)
                    .body("FOOD", equalTo(42.50f))
                    .body("BILLS", equalTo(64.30f))
                    .body("TRAVEL", equalTo(0.00f))
                    .body("SHOPPING", equalTo(0.00f))
                    .body("OTHER", equalTo(0.00f));
        }
    }

    @Nested
    class XmlExport {

        @Test
        void exportsCallersExpensesInLegacyFormat() {
            create(ALICE, "Weekly groceries", "82.4", "FOOD", "2026-11-03");
            create(ALICE, "Flight to Bangalore", "5499", "TRAVEL", "2026-10-15");
            create(BOB, "Not mine", "1", "OTHER", "2026-11-01");

            given().auth().oauth2(Tokens.validFor(ALICE))
                    .get("/api/expenses/export/xml")
                    .then().statusCode(200)
                    .contentType(startsWith("application/xml"))
                    .body("Expenses.Expense.size()", equalTo(2))
                    .body("Expenses.Expense[0].Description", equalTo("WEEKLY GROCERIES"))
                    .body("Expenses.Expense[0].Amt", equalTo("82.40"))
                    .body("Expenses.Expense[0].CatCode", equalTo("F01"))
                    .body("Expenses.Expense[0].TxnDate", equalTo("03-11-2026"))
                    .body("Expenses.Expense[1].Description", equalTo("FLIGHT TO BANGALORE"))
                    .body("Expenses.Expense[1].Amt", equalTo("5499.00"))
                    .body("Expenses.Expense[1].CatCode", equalTo("T01"))
                    .body("Expenses.Expense[1].TxnDate", equalTo("15-10-2026"));
        }

        @Test
        void requiresJwt() {
            given().get("/api/expenses/export/xml").then().statusCode(401);
        }
    }

    @Nested
    class Validation {

        @Test
        void invalidBodyReturns400WithEveryFieldError() {
            as(ALICE).body(body("  ", -5, "PETS", LocalDate.now().plusDays(1).toString()))
                    .post("/api/expenses")
                    .then().statusCode(400)
                    .body("code", equalTo("VALIDATION_FAILED"))
                    .body("message", startsWith("Validation failed"))
                    .body("timestamp", notNullValue())
                    .body("details.field", containsInAnyOrder("amount", "category", "expenseDate", "title"))
                    .body("details.find { it.field == 'expenseDate' }.message",
                            equalTo("Expense date must not be in the future"))
                    .body("details.find { it.field == 'category' }.message",
                            equalTo("Category must be one of FOOD, TRAVEL, BILLS, SHOPPING, OTHER"));
        }

        @Test
        void zeroAmountIsRejected() {
            as(ALICE).body(body("Free sample", 0, "FOOD", TODAY)).post("/api/expenses")
                    .then().statusCode(400)
                    .body("details[0].field", equalTo("amount"))
                    .body("details[0].message", equalTo("Amount must be greater than zero"));
        }

        @Test
        void missingFieldsAreRejected() {
            as(ALICE).body(Map.of()).post("/api/expenses")
                    .then().statusCode(400)
                    .body("details.field", containsInAnyOrder("amount", "category", "expenseDate", "title"));
        }

        @Test
        void lowercaseCategoryIsRejected() {
            as(ALICE).body(body("Lunch", 10, "food", TODAY)).post("/api/expenses")
                    .then().statusCode(400).body("details[0].field", equalTo("category"));
        }

        @Test
        void malformedJsonReturns400() {
            as(ALICE).body("{\"title\": \"oops\", \"amount\": ").post("/api/expenses")
                    .then().statusCode(400).body("code", equalTo("MALFORMED_REQUEST"));
        }

        @Test
        void badDateFormatReturns400() {
            as(ALICE).body(body("Lunch", 10, "FOOD", "03-11-2026")).post("/api/expenses")
                    .then().statusCode(400).body("code", equalTo("MALFORMED_REQUEST"));
        }
    }

    @Nested
    class NotFound {

        @Test
        void unknownIdReturns404InSpecErrorFormat() {
            as(ALICE).get("/api/expenses/{id}", 999_999).then().statusCode(404)
                    .body("code", equalTo("EXPENSE_NOT_FOUND"))
                    .body("message", equalTo("Expense 999999 not found"))
                    .body("timestamp", notNullValue());
        }

        @Test
        void anotherUsersExpenseIsInvisible() {
            long id = create(ALICE, "Private expense", "99.00", "OTHER", "2026-11-01");

            as(BOB).get("/api/expenses/{id}", id).then().statusCode(404);
            as(BOB).body(body("Hijack", 1, "OTHER", TODAY)).put("/api/expenses/{id}", id).then().statusCode(404);
            as(BOB).delete("/api/expenses/{id}", id).then().statusCode(404);

            as(ALICE).get("/api/expenses/{id}", id).then().statusCode(200).body("title", equalTo("Private expense"));
        }
    }

    @Nested
    class Security {

        @Test
        void noTokenReturns401InSpecErrorFormat() {
            given().get("/api/expenses").then().statusCode(401)
                    .header("WWW-Authenticate", "Bearer")
                    .body("code", equalTo("UNAUTHORIZED"));
            given().contentType(ContentType.JSON).body(body("x", 1, "FOOD", TODAY))
                    .post("/api/expenses").then().statusCode(401);
        }

        @Test
        void tokenSignedWithUntrustedKeyIsRejected() {
            given().auth().oauth2(Tokens.signedWithUntrustedKey(ALICE)).get("/api/expenses").then().statusCode(401);
        }

        @Test
        void tamperedTokenIsRejected() {
            String[] alice = Tokens.validFor(ALICE).split("\\.");
            String[] bob = Tokens.validFor(BOB).split("\\.");
            given().auth().oauth2(alice[0] + "." + bob[1] + "." + alice[2]).get("/api/expenses").then().statusCode(401);
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
        void tokenWithoutUserGroupReturns403() {
            given().auth().oauth2(Tokens.withoutUserRole(ALICE)).get("/api/expenses").then().statusCode(403)
                    .body("code", equalTo("FORBIDDEN"));
        }
    }
}
