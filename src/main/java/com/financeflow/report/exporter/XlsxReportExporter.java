package com.financeflow.report.exporter;

import com.financeflow.report.ReportContext;
import com.financeflow.report.ReportFormat;
import com.financeflow.transaction.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Component
public class XlsxReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override public ReportFormat format() { return ReportFormat.XLSX; }
    @Override public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
    @Override public String fileExtension() { return "xlsx"; }

    @Override
    public byte[] export(ReportContext ctx) throws IOException {
        try (var wb = new XSSFWorkbook(); var baos = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("Transações");

            // Estilos
            var boldFont = wb.createFont(); boldFont.setBold(true);
            var headerStyle = wb.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            var moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            // Cabeçalho do relatório
            var titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(ctx.title());
            titleRow.getCell(0).setCellStyle(headerStyle);

            var periodRow = sheet.createRow(1);
            periodRow.createCell(0).setCellValue("Período:");
            periodRow.createCell(1).setCellValue(
                    ctx.from().format(DATE_FMT) + " até " + ctx.to().format(DATE_FMT));

            // Cabeçalho da tabela
            var headerRow = sheet.createRow(3);
            String[] headers = {"Data", "Descrição", "Categoria", "Conta", "Tipo", "Direção", "Valor"};
            for (int i = 0; i < headers.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            BigDecimal credits = BigDecimal.ZERO, debits = BigDecimal.ZERO;

            for (Transaction t : ctx.transactions()) {
                var row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getTransactionDate().format(DATE_FMT));
                row.createCell(1).setCellValue(t.getDescription());
                row.createCell(2).setCellValue(t.getCategory() == null ? "" : t.getCategory().getName());
                row.createCell(3).setCellValue(t.getAccount().getName());
                row.createCell(4).setCellValue(t.getType().toString());
                row.createCell(5).setCellValue(t.getDirection().toString());

                var amountCell = row.createCell(6);
                amountCell.setCellValue(t.getAmount().doubleValue());
                amountCell.setCellStyle(moneyStyle);

                if ("CREDIT".equals(t.getDirection().toString())) credits = credits.add(t.getAmount());
                else debits = debits.add(t.getAmount());
            }

            // Totais
            rowIdx++;
            addSummaryRow(sheet, rowIdx++, "Total créditos", credits, boldFont, moneyStyle);
            addSummaryRow(sheet, rowIdx++, "Total débitos", debits, boldFont, moneyStyle);
            addSummaryRow(sheet, rowIdx, "Resultado", credits.subtract(debits), boldFont, moneyStyle);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private void addSummaryRow(Sheet sheet, int rowIdx, String label,
                               BigDecimal value, Font bold, CellStyle moneyStyle) {
        var row = sheet.createRow(rowIdx);
        var labelCell = row.createCell(5);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(createBoldStyle(sheet.getWorkbook(), bold));
        var valueCell = row.createCell(6);
        valueCell.setCellValue(value.doubleValue());
        valueCell.setCellStyle(moneyStyle);
    }

    private CellStyle createBoldStyle(Workbook wb, Font bold) {
        var style = wb.createCellStyle();
        style.setFont(bold);
        return style;
    }
}