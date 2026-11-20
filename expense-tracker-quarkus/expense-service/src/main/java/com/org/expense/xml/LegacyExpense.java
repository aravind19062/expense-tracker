package com.org.expense.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * One expense in the format of the legacy accounting system:
 * {@code <Expense><Description/><Amt/><CatCode/><TxnDate/></Expense>}.
 * All values are already formatted strings - the formatting rules live in ExpenseMapper.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Expense", propOrder = {"description", "amt", "catCode", "txnDate"})
public class LegacyExpense {

    @XmlElement(name = "Description")
    private String description;

    @XmlElement(name = "Amt")
    private String amt;

    @XmlElement(name = "CatCode")
    private String catCode;

    @XmlElement(name = "TxnDate")
    private String txnDate;

    public LegacyExpense() {
        // required by JAXB
    }

    public LegacyExpense(String description, String amt, String catCode, String txnDate) {
        this.description = description;
        this.amt = amt;
        this.catCode = catCode;
        this.txnDate = txnDate;
    }

    public String getDescription() {
        return description;
    }

    public String getAmt() {
        return amt;
    }

    public String getCatCode() {
        return catCode;
    }

    public String getTxnDate() {
        return txnDate;
    }
}
