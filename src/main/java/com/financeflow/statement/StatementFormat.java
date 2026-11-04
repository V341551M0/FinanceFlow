package com.financeflow.statement;

public enum StatementFormat {
    CSV, OFX, QIF, PDF;

    public static StatementFormat fromFilename(String name) {
        if (name == null) return null;
        var lower = name.toLowerCase();
        if (lower.endsWith(".csv")) return CSV;
        if (lower.endsWith(".ofx") || lower.endsWith(".qfx")) return OFX;
        if (lower.endsWith(".qif")) return QIF;
        if (lower.endsWith(".pdf")) return PDF;
        return null;
    }
}