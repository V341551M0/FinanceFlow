package com.financeflow.statement.importer;

import com.financeflow.statement.*;
import com.financeflow.transaction.TransactionDirection;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class CsvStatementImporter implements StatementImporter {

    private static final Set<String> DATE_KEYS = Set.of("date", "data", "dt", "transaction_date");
    private static final Set<String> DESC_KEYS = Set.of("description", "descricao", "descrição", "memo", "historico", "histórico");
    private static final Set<String> AMOUNT_KEYS = Set.of("amount", "valor", "value", "montante");
    private static final Set<String> BALANCE_KEYS = Set.of("balance", "saldo");

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
    };

    @Override public StatementFormat format() { return StatementFormat.CSV; }

    @Override
    public ParsedStatement parse(InputStream in, Long accountId) throws IOException {
        var content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        if (content.isBlank()) throw new IOException("Arquivo CSV vazio.");

        char delimiter = content.contains(";") ? ';' : ',';
        var format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true).setTrim(true)
                .setDelimiter(delimiter).build();

        List<ParsedTransaction> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        BigDecimal declaredFinalBalance = null;

        try (CSVParser parser = CSVParser.parse(content, format)) {
            var headers = parser.getHeaderMap().keySet();
            String dateCol = find(headers, DATE_KEYS);
            String descCol = find(headers, DESC_KEYS);
            String amountCol = find(headers, AMOUNT_KEYS);
            String balanceCol = find(headers, BALANCE_KEYS);

            if (dateCol == null || descCol == null || amountCol == null) {
                throw new IOException(
                        "Cabeçalho inválido. Colunas obrigatórias: data, descrição, valor.");
            }

            for (CSVRecord r : parser) {
                String dateRaw = r.get(dateCol);
                String desc = r.get(descCol);
                String amountRaw = r.get(amountCol);

                ParsedTransaction parsed = parseRow(dateRaw, desc, amountRaw, accountId);
                rows.add(parsed);

                if (balanceCol != null) {
                    try {
                        declaredFinalBalance = parseMoney(r.get(balanceCol));
                    } catch (Exception ignored) { /* último valor válido prevalece */ }
                }
            }
        }

        return new ParsedStatement(format(), rows, declaredFinalBalance, warnings);
    }

    private ParsedTransaction parseRow(String dateRaw, String desc, String amountRaw, Long accountId) {
        try {
            LocalDate date = parseDate(dateRaw);
            BigDecimal signed = parseMoney(amountRaw);
            var direction = signed.signum() < 0 ? TransactionDirection.DEBIT : TransactionDirection.CREDIT;
            BigDecimal amount = signed.abs();
            String ext = hashExternal(accountId, date, desc, amount, direction);
            return new ParsedTransaction(date, desc.trim(), amount, direction, ext, null);
        } catch (Exception e) {
            return new ParsedTransaction(null, desc, null, null, null, e.getMessage());
        }
    }

    private LocalDate parseDate(String s) {
        for (var f : DATE_FORMATS) {
            try { return LocalDate.parse(s.trim(), f); }
            catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException("Data inválida: " + s);
    }

    private BigDecimal parseMoney(String s) {
        var clean = s.replaceAll("[R$\\s]", "").trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Valor vazio");
        if (clean.contains(",")) {
            // Formato brasileiro: 1.234,56 → remover pontos de milhar e trocar vírgula por ponto
            clean = clean.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(clean);
    }

    private String find(Set<String> headers, Set<String> keys) {
        return headers.stream()
                .filter(h -> keys.contains(h.toLowerCase(Locale.ROOT)))
                .findFirst().orElse(null);
    }

    private String hashExternal(Long accountId, LocalDate date, String desc, BigDecimal amt, TransactionDirection dir) {
        try {
            var raw = accountId + "|" + date + "|" + desc.trim().toLowerCase(Locale.ROOT)
                    + "|" + amt.toPlainString() + "|" + dir;
            var md = MessageDigest.getInstance("SHA-256");
            var hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.substring(0, 32);
        } catch (Exception e) {
            return null;
        }
    }
}