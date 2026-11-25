package com.financeflow.report;

import com.financeflow.transaction.Transaction;

import java.time.LocalDate;
import java.util.List;

public record ReportContext(
        String title,
        LocalDate from,
        LocalDate to,
        String userName,
        String userEmail,
        List<Transaction> transactions
) {}