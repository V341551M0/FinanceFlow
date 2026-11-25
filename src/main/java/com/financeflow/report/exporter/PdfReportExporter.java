package com.financeflow.report.exporter;

import com.financeflow.report.ReportContext;
import com.financeflow.report.ReportFormat;
import com.financeflow.transaction.Transaction;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Component
public class PdfReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb BRAND_BLUE = new DeviceRgb(59, 130, 246);
    private static final DeviceRgb LIGHT_GREY = new DeviceRgb(241, 245, 249);
    private static final DeviceRgb POSITIVE = new DeviceRgb(34, 197, 94);
    private static final DeviceRgb NEGATIVE = new DeviceRgb(239, 68, 68);

    @Override public ReportFormat format() { return ReportFormat.PDF; }
    @Override public String contentType() { return "application/pdf"; }
    @Override public String fileExtension() { return "pdf"; }

    @Override
    public byte[] export(ReportContext ctx) throws IOException {
        var baos = new ByteArrayOutputStream();
        try (var pdf = new PdfDocument(new PdfWriter(baos));
             var doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);

            // Cabeçalho
            doc.add(new Paragraph("FinanceFlow")
                    .setFontSize(20).setBold().setFontColor(BRAND_BLUE)
                    .setMarginBottom(0));
            doc.add(new Paragraph(ctx.title())
                    .setFontSize(12).setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginBottom(2));
            doc.add(new Paragraph("Período: " + ctx.from().format(DATE_FMT)
                    + " até " + ctx.to().format(DATE_FMT))
                    .setFontSize(10).setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(16));

            // Resumo
            var credits = BigDecimal.ZERO;
            var debits = BigDecimal.ZERO;
            for (Transaction t : ctx.transactions()) {
                if ("CREDIT".equals(t.getDirection().toString())) credits = credits.add(t.getAmount());
                else debits = debits.add(t.getAmount());
            }

            var summary = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(16);
            summary.addCell(summaryCell("Receitas", credits, POSITIVE));
            summary.addCell(summaryCell("Despesas", debits, NEGATIVE));
            summary.addCell(summaryCell("Resultado", credits.subtract(debits),
                    credits.subtract(debits).signum() >= 0 ? POSITIVE : NEGATIVE));
            doc.add(summary);

            // Tabela de transações
            var table = new Table(UnitValue.createPercentArray(new float[]{2, 5, 3, 2, 2}))
                    .setWidth(UnitValue.createPercentValue(100));

            for (String h : new String[]{"Data", "Descrição", "Categoria", "Tipo", "Valor"}) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setBold().setFontSize(9))
                        .setBackgroundColor(LIGHT_GREY)
                        .setBorder(Border.NO_BORDER)
                        .setPadding(6));
            }

            for (Transaction t : ctx.transactions()) {
                table.addCell(bodyCell(t.getTransactionDate().format(DATE_FMT)));
                table.addCell(bodyCell(t.getDescription()));
                table.addCell(bodyCell(t.getCategory() == null ? "—" : t.getCategory().getName()));
                table.addCell(bodyCell(t.getType().toString()));
                var amount = t.getAmount().toPlainString();
                var amountCell = new Cell()
                        .add(new Paragraph(amount).setFontSize(9)
                                .setTextAlignment(TextAlignment.RIGHT)
                                .setFontColor("CREDIT".equals(t.getDirection().toString())
                                        ? POSITIVE : NEGATIVE))
                        .setBorder(Border.NO_BORDER)
                        .setPadding(5);
                table.addCell(amountCell);
            }

            doc.add(table);

            if (ctx.transactions().isEmpty()) {
                doc.add(new Paragraph("Nenhuma transação no período selecionado.")
                        .setFontSize(10).setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(24));
            }

            // Rodapé
            doc.add(new Paragraph("Gerado por FinanceFlow — " + ctx.userEmail())
                    .setFontSize(8).setFontColor(ColorConstants.GRAY)
                    .setMarginTop(24).setHorizontalAlignment(HorizontalAlignment.RIGHT));
        }

        return baos.toByteArray();
    }

    private Cell summaryCell(String label, BigDecimal value, DeviceRgb color) {
        return new Cell()
                .add(new Paragraph(label).setFontSize(9).setFontColor(ColorConstants.GRAY))
                .add(new Paragraph(value.toPlainString())
                        .setFontSize(14).setBold().setFontColor(color))
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(LIGHT_GREY)
                .setPadding(10);
    }

    private Cell bodyCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setPadding(5);
    }
}