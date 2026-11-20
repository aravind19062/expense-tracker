package com.org.expense.mapper;

import com.org.expense.dto.ExpenseResponse;
import com.org.expense.entity.Category;
import com.org.expense.xml.LegacyExpense;
import com.org.expense.xml.LegacyExpenseExport;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain JUnit - no Quarkus, no database. One test per mapping rule. */
class ExpenseMapperTest {

    private final ExpenseMapper mapper = new ExpenseMapper();

    private static ExpenseResponse expense(String title, String amount, Category category, LocalDate date) {
        return new ExpenseResponse(7L, "101", title, amount == null ? null : new BigDecimal(amount), category, date,
                Instant.parse("2026-11-20T10:00:00Z"));
    }

    @Test
    void mapsAllFourFields() {
        LegacyExpense xml = mapper.toLegacy(expense("Weekly groceries", "82.45", Category.FOOD, LocalDate.of(2026, 11, 3)));

        assertEquals("WEEKLY GROCERIES", xml.getDescription());
        assertEquals("82.45", xml.getAmt());
        assertEquals("F01", xml.getCatCode());
        assertEquals("03-11-2026", xml.getTxnDate());
    }

    @Nested
    class Description {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "taxi to airport        | TAXI TO AIRPORT",
                "Already UPPER          | ALREADY UPPER",
                "café latte             | CAFÉ LATTE",
                "Rent - Nov #2          | RENT - NOV #2"})
        void isUppercased(String title, String expected) {
            assertEquals(expected, ExpenseMapper.description(title));
        }

        @Test
        void usesLocaleIndependentUppercase() {
            // With a Turkish default locale, "i".toUpperCase() would be "İ" (dotted capital I).
            assertEquals("BILLS", ExpenseMapper.description("bills"));
        }
    }

    @Nested
    class Amt {

        @ParameterizedTest
        @CsvSource({
                "10,      10.00",
                "10.5,    10.50",
                "82.45,   82.45",
                "0.1,     0.10",
                "3.456,   3.46",
                "3.455,   3.46",
                "3.454,   3.45",
                "1E+3,    1000.00",
                "12345678.99, 12345678.99"})
        void hasExactlyTwoDecimals(String amount, String expected) {
            assertEquals(expected, ExpenseMapper.amt(new BigDecimal(amount)));
        }
    }

    @Nested
    class CatCode {

        @ParameterizedTest
        @CsvSource({"FOOD, F01", "TRAVEL, T01", "BILLS, B01", "SHOPPING, S01", "OTHER, O01"})
        void mapsEveryCategory(Category category, String code) {
            assertEquals(code, ExpenseMapper.catCode(category));
        }

        @Test
        void everyCategoryHasACode() {
            for (Category c : Category.values()) {
                assertTrue(ExpenseMapper.catCode(c).matches("[A-Z]\\d\\d"), c.name());
            }
        }
    }

    @Nested
    class TxnDate {

        @ParameterizedTest
        @CsvSource({
                "2026-11-03, 03-11-2026",
                "2026-01-31, 31-01-2026",
                "2024-02-29, 29-02-2024",
                "1999-12-01, 01-12-1999"})
        void isFormattedDdMmYyyy(LocalDate date, String expected) {
            assertEquals(expected, ExpenseMapper.txnDate(date));
        }
    }

    @Test
    void nullFieldsBecomeMissingElements() {
        LegacyExpense xml = mapper.toLegacy(expense(null, null, null, null));

        assertNull(xml.getDescription());
        assertNull(xml.getAmt());
        assertNull(xml.getCatCode());
        assertNull(xml.getTxnDate());
    }

    @Test
    void exportKeepsOrderAndCount() {
        LegacyExpenseExport export = mapper.toLegacyExport(List.of(
                expense("a", "1", Category.FOOD, LocalDate.of(2026, 1, 1)),
                expense("b", "2", Category.BILLS, LocalDate.of(2026, 1, 2))));

        assertEquals(2, export.getExpenses().size());
        assertEquals("A", export.getExpenses().get(0).getDescription());
        assertEquals("B01", export.getExpenses().get(1).getCatCode());
    }

    @Test
    void marshalsToTheLegacyXmlLayout() throws Exception {
        LegacyExpenseExport export = mapper.toLegacyExport(List.of(
                expense("Flight to Bangalore", "5499.9", Category.TRAVEL, LocalDate.of(2026, 10, 15))));

        Marshaller marshaller = JAXBContext.newInstance(LegacyExpenseExport.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter out = new StringWriter();
        marshaller.marshal(export, out);

        assertEquals("<Expenses><Expense>"
                + "<Description>FLIGHT TO BANGALORE</Description>"
                + "<Amt>5499.90</Amt>"
                + "<CatCode>T01</CatCode>"
                + "<TxnDate>15-10-2026</TxnDate>"
                + "</Expense></Expenses>", out.toString());
    }

    @Test
    void emptyExportIsAnEmptyRoot() throws Exception {
        Marshaller marshaller = JAXBContext.newInstance(LegacyExpenseExport.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter out = new StringWriter();
        marshaller.marshal(mapper.toLegacyExport(List.of()), out);

        assertEquals("<Expenses/>", out.toString());
    }
}
