package com.financeflow.report.exporter;

import com.financeflow.report.ReportContext;
import com.financeflow.report.ReportFormat;

import java.io.IOException;

public interface ReportExporter {
    ReportFormat format();
    byte[] export(ReportContext context) throws IOException;
    String contentType();
    String fileExtension();
}