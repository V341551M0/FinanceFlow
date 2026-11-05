package com.financeflow.report;

import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.report.exporter.ReportExporter;
import com.financeflow.transaction.TransactionRepository;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final Map<ReportFormat, ReportExporter> exporters;
    private final TransactionRepository transactions;
    private final UserRepository users;

    public ReportService(List<ReportExporter> exporterList,
                         TransactionRepository transactions,
                         UserRepository users) {
        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(ReportExporter::format, Function.identity()));
        this.transactions = transactions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public ExportResult export(Long userId, ReportFormat format, LocalDate from, LocalDate to)
            throws IOException {
        if (from.isAfter(to)) throw new BusinessException("Data inicial deve ser anterior à final.");

        var user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        var txs = transactions.findForReport(userId, from, to);

        var ctx = new ReportContext(
                "Relatório Financeiro — " + user.getName(),
                from, to, user.getName(), user.getEmail(), txs);

        var exporter = exporters.get(format);
        if (exporter == null) throw new BusinessException("Formato não suportado: " + format);

        var bytes = exporter.export(ctx);
        var filename = String.format("financeflow-%s-%s.%s",
                from, to, exporter.fileExtension());

        return new ExportResult(bytes, exporter.contentType(), filename);
    }

    public record ExportResult(byte[] bytes, String contentType, String filename) {}
}