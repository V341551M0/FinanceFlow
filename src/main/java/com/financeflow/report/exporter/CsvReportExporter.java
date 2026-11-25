package com.financeflow.report.exporter;

import com.financeflow.report.ReportContext;
import com.financeflow.report.ReportFormat;
import com.financeflow.transaction.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Component
public class CsvReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override public ReportFormat format() { return ReportFormat.CSV; }
    @Override public String contentType() { return "text/csv; charset=UTF-8"; }
    @Override public String fileExtension() { return "csv"; }

    @Override
    public byte[] export(ReportContext ctx) throws IOException {
        var baos = new ByteArrayOutputStream();
        // BOM para Excel reconhecer UTF-8
        baos.write(0xEF); baos.write(0xBB); baos.write(0xBF);

        try (var writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             var printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setDelimiter(';')
                     .setHeader("Data", "Descrição", "Categoria", "Conta",
                             "Tipo", "Direção", "Valor")
                     .build())) {

            BigDecimal credits = BigDecimal.ZERO, debits = BigDecimal.ZERO;

            for (Transaction t : ctx.transactions()) {
                var amount = t.getAmount();
                if ("CREDIT".equals(String.valueOf(t.getDirection()))) credits = credits.add(amount);
                else debits = debits.add(amount);

                printer.printRecord(
                        t.getTransactionDate().format(DATE_FMT),
                        t.getDescription(),
                        t.getCategory() == null ? "" : t.getCategory().getName(),
                        t.getAccount().getName(),
                        t.getType(),
                        t.getDirection(),
                        amount
                );
            }

            printer.println();
            printer.printRecord("", "", "", "", "", "Total créditos", credits);
            printer.printRecord("", "", "", "", "", "Total débitos", debits);
            printer.printRecord("", "", "", "", "", "Resultado", credits.subtract(debits));
        }

        return baos.toByteArray();
    }
}