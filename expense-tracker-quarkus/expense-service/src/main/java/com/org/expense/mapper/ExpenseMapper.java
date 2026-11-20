package com.org.expense.mapper;

import com.org.expense.dto.ExpenseResponse;
import com.org.expense.entity.Category;
import com.org.expense.xml.LegacyExpense;
import com.org.expense.xml.LegacyExpenseExport;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Maps the JSON expense representation to the legacy accounting system's XML record.
 *
 * <pre>
 * JSON field   XML element    Rule
 * title        Description    UPPERCASE
 * amount       Amt            exactly 2 decimal places (HALF_UP)
 * category     CatCode        FOOD-F01, TRAVEL-T01, BILLS-B01, SHOPPING-S01, OTHER-O01
 * expenseDate  TxnDate        dd-MM-yyyy
 * </pre>
 *
 * No framework dependencies, so it is unit-tested with plain JUnit. A null input field becomes a
 * missing XML element instead of failing the whole export.
 */
@ApplicationScoped
public class ExpenseMapper {

    static final DateTimeFormatter TXN_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public LegacyExpense toLegacy(ExpenseResponse expense) {
        return new LegacyExpense(
                description(expense.title()),
                amt(expense.amount()),
                catCode(expense.category()),
                txnDate(expense.expenseDate()));
    }

    public LegacyExpenseExport toLegacyExport(List<ExpenseResponse> expenses) {
        return new LegacyExpenseExport(expenses.stream().map(this::toLegacy).toList());
    }

    static String description(String title) {
        return title == null ? null : title.toUpperCase(Locale.ROOT);
    }

    static String amt(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String catCode(Category category) {
        if (category == null) {
            return null;
        }
        // An exhaustive switch: adding a Category without a code is a compile error.
        return switch (category) {
            case FOOD -> "F01";
            case TRAVEL -> "T01";
            case BILLS -> "B01";
            case SHOPPING -> "S01";
            case OTHER -> "O01";
        };
    }

    static String txnDate(LocalDate date) {
        return date == null ? null : TXN_DATE.format(date);
    }
}
