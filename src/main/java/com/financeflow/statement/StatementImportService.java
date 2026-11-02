package com.financeflow.statement;

import com.financeflow.account.FinancialAccount;
import com.financeflow.account.FinancialAccountRepository;
import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.statement.dto.*;
import com.financeflow.transaction.*;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatementImportService {

    private final Map<StatementFormat, StatementImporter> importers;
    private final FinancialAccountRepository accounts;
    private final TransactionRepository transactions;
    private final UserRepository users;
    private final TransactionService transactionService;

    public StatementImportService(List<StatementImporter> importerList,
                                  FinancialAccountRepository accounts,
                                  TransactionRepository transactions,
                                  UserRepository users,
                                  TransactionService transactionService) {
        this.importers = importerList.stream()
                .collect(Collectors.toMap(StatementImporter::format, Function.identity()));
        this.accounts = accounts;
        this.transactions = transactions;
        this.users = users;
        this.transactionService = transactionService;
    }

    public StatementPreviewResponse preview(Long userId, Long accountId, MultipartFile file) {
        var account = accounts.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));

        var format = StatementFormat.fromFilename(file.getOriginalFilename());
        if (format == null) throw new BusinessException("Extensão de arquivo não suportada.");
        var importer = importers.get(format);
        if (importer == null) throw new BusinessException("Importador não disponível para " + format);

        ParsedStatement parsed;
        try (InputStream in = file.getInputStream()) {
            parsed = importer.parse(in, accountId);
        } catch (IOException e) {
            throw new BusinessException("Falha ao ler o arquivo: " + e.getMessage());
        }

        List<PreviewRowResponse> rows = new ArrayList<>();
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal debits = BigDecimal.ZERO;
        int valid = 0, invalid = 0, duplicates = 0;

        for (var t : parsed.transactions()) {
            boolean isDup = false;
            if (t.isValid()) {
                isDup = transactions.findByAccountAndExternalId(userId, accountId, t.externalId()).isPresent();
                if (isDup) duplicates++;
                else {
                    valid++;
                    if (t.direction() == TransactionDirection.CREDIT) credits = credits.add(t.amount());
                    else debits = debits.add(t.amount());
                }
            } else invalid++;

            rows.add(new PreviewRowResponse(
                    t.date(), t.description(), t.amount(), t.direction(),
                    t.isValid(), isDup, t.error()));
        }

        BigDecimal projected = account.getCurrentBalance().add(credits).subtract(debits);
        boolean reconciled = parsed.declaredFinalBalance() == null ||
                projected.subtract(parsed.declaredFinalBalance()).abs().compareTo(new BigDecimal("0.01")) < 0;

        return new StatementPreviewResponse(
                format.name(), rows.size(), valid, invalid, duplicates,
                parsed.declaredFinalBalance(), projected, reconciled,
                parsed.warnings(), rows);
    }

    @Transactional
    public ImportResultResponse importFile(Long userId, Long accountId, MultipartFile file) {
        var account = accounts.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));

        var format = StatementFormat.fromFilename(file.getOriginalFilename());
        if (format == null) throw new BusinessException("Extensão de arquivo não suportada.");
        var importer = importers.get(format);
        if (importer == null) throw new BusinessException("Importador não disponível para " + format);

        ParsedStatement parsed;
        try (InputStream in = file.getInputStream()) {
            parsed = importer.parse(in, accountId);
        } catch (IOException e) {
            throw new BusinessException("Falha ao ler o arquivo: " + e.getMessage());
        }

        var user = users.findById(userId).orElseThrow();
        int imported = 0, skippedDup = 0, skippedInvalid = 0;

        for (var t : parsed.transactions()) {
            if (!t.isValid()) { skippedInvalid++; continue; }
            if (transactions.findByAccountAndExternalId(userId, accountId, t.externalId()).isPresent()) {
                skippedDup++; continue;
            }
            var tx = Transaction.builder()
                    .user(user).account(account)
                    .type(t.direction() == TransactionDirection.CREDIT
                            ? TransactionType.INCOME : TransactionType.EXPENSE)
                    .direction(t.direction())
                    .amount(t.amount())
                    .transactionDate(t.date())
                    .description(t.description())
                    .source(format == StatementFormat.CSV
                            ? TransactionSource.CSV : TransactionSource.OFX)
                    .status(TransactionStatus.POSTED)
                    .externalId(t.externalId())
                    .build();
            transactions.save(tx);
            imported++;
        }

        transactionService.recalculateBalance(accountId);
        var refreshed = accounts.findById(accountId).orElseThrow();

        boolean reconciled = parsed.declaredFinalBalance() == null ||
                refreshed.getCurrentBalance().subtract(parsed.declaredFinalBalance())
                        .abs().compareTo(new BigDecimal("0.01")) < 0;

        return new ImportResultResponse(
                imported, skippedDup, skippedInvalid,
                refreshed.getCurrentBalance(), parsed.declaredFinalBalance(), reconciled);
    }
}