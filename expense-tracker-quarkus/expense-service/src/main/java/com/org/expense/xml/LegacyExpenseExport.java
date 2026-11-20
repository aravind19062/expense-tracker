package com.org.expense.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

/** Root of the XML export: {@code <Expenses><Expense>...</Expense>...</Expenses>}. */
@XmlRootElement(name = "Expenses")
@XmlAccessorType(XmlAccessType.FIELD)
public class LegacyExpenseExport {

    @XmlElement(name = "Expense")
    private List<LegacyExpense> expenses = new ArrayList<>();

    public LegacyExpenseExport() {
        // required by JAXB
    }

    public LegacyExpenseExport(List<LegacyExpense> expenses) {
        this.expenses = new ArrayList<>(expenses);
    }

    public List<LegacyExpense> getExpenses() {
        return expenses;
    }
}
