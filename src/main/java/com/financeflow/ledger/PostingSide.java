package com.financeflow.ledger;

public enum PostingSide {
    DEBIT,
    CREDIT;

    public PostingSide opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
