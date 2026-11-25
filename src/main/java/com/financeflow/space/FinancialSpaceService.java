package com.financeflow.space;

import com.financeflow.account.FinancialAccount;
import com.financeflow.account.FinancialAccountRepository;
import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.ledger.DoubleEntryLedgerService;
import com.financeflow.ledger.DoubleEntryLedgerService.PostEntryCommand;
import com.financeflow.ledger.DoubleEntryLedgerService.PostingLine;
import com.financeflow.ledger.JournalEntry;
import com.financeflow.ledger.JournalEntryStatus;
import com.financeflow.ledger.LedgerAccount;
import com.financeflow.ledger.LedgerAccountClass;
import com.financeflow.ledger.PostingSide;
import com.financeflow.ledger.RecordOrigin;
import com.financeflow.space.dto.*;
import com.financeflow.transaction.Transaction;
import com.financeflow.transaction.TransactionRepository;
import com.financeflow.transaction.TransactionStatus;
import com.financeflow.transaction.TransactionType;
import com.financeflow.user.User;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FinancialSpaceService {

    private final FinancialSpaceRepository spaces;
    private final SpaceTransferRepository transfers;
    private final SpaceAnalyticsRepository analytics;
    private final FinancialAccountRepository accounts;
    private final TransactionRepository transactions;
    private final UserRepository users;
    private final DoubleEntryLedgerService ledger;

    public FinancialSpaceService(FinancialSpaceRepository spaces,
                                 SpaceTransferRepository transfers,
                                 SpaceAnalyticsRepository analytics,
                                 FinancialAccountRepository accounts,
                                 TransactionRepository transactions,
                                 UserRepository users,
                                 DoubleEntryLedgerService ledger) {
        this.spaces = spaces;
        this.transfers = transfers;
        this.analytics = analytics;
        this.accounts = accounts;
        this.transactions = transactions;
        this.users = users;
        this.ledger = ledger;
    }

    @Transactional
    public FinancialSpace ensureDefaultPersonalSpace(Long userId) {
        return spaces.findByUserIdAndDefaultSpaceTrue(userId)
                .orElseGet(() -> {
                    User user = users.findById(userId)
                            .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
                    String currency = user.getCurrency() != null ? user.getCurrency() : "BRL";
                    return spaces.save(FinancialSpace.builder()
                            .user(user)
                            .name("Finanças Pessoais")
                            .slug("pessoal")
                            .type(SpaceType.PERSONAL)
                            .description("Espaço financeiro pessoal principal")
                            .currency(currency)
                            .color("#479FFA")
                            .icon("wallet")
                            .defaultSpace(true)
                            .active(true)
                            .build());
                });
    }

    @Transactional
    public SpaceResponse create(Long userId, CreateSpaceRequest req) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        ensureDefaultPersonalSpace(userId);

        String currency = (req.currency() != null && !req.currency().isBlank())
                ? req.currency().trim().toUpperCase(Locale.ROOT)
                : (user.getCurrency() != null ? user.getCurrency() : "BRL");

        FinancialAccount defaultAccount = resolveDefaultAccount(userId, req.defaultAccountId());
        String slug = generateUniqueSlug(userId, req.name());

        FinancialSpace space = FinancialSpace.builder()
                .user(user)
                .name(req.name().trim())
                .slug(slug)
                .type(req.type())
                .description(req.description() != null ? req.description().trim() : null)
                .currency(currency)
                .color(req.color())
                .icon(req.icon())
                .defaultSpace(false)
                .active(true)
                .defaultAccount(defaultAccount)
                .build();

        return SpaceResponse.from(spaces.save(space));
    }

    @Transactional
    public List<SpaceResponse> list(Long userId) {
        ensureDefaultPersonalSpace(userId);
        return spaces.findAllByUserIdAndActiveTrueOrderByDefaultSpaceDescNameAsc(userId).stream()
                .map(SpaceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpaceResponse get(Long userId, Long spaceId) {
        FinancialSpace space = spaces.findByIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new NotFoundException("Espaço financeiro não encontrado"));
        return SpaceResponse.from(space);
    }

    @Transactional
    public SpaceResponse update(Long userId, Long spaceId, UpdateSpaceRequest req) {
        FinancialSpace space = spaces.findByIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new NotFoundException("Espaço financeiro não encontrado"));

        space.setName(req.name().trim());
        if (!Boolean.TRUE.equals(space.getDefaultSpace())) {
            space.setType(req.type());
        }
        space.setDescription(req.description() != null ? req.description().trim() : null);
        space.setColor(req.color());
        space.setIcon(req.icon());
        space.setDefaultAccount(resolveDefaultAccount(userId, req.defaultAccountId()));

        return SpaceResponse.from(spaces.save(space));
    }

    @Transactional
    public void archive(Long userId, Long spaceId) {
        FinancialSpace space = spaces.findByIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new NotFoundException("Espaço financeiro não encontrado"));

        if (Boolean.TRUE.equals(space.getDefaultSpace())) {
            throw new BusinessException("O espaço financeiro pessoal padrão não pode ser arquivado.");
        }

        space.setActive(false);
        spaces.save(space);
    }

    @Transactional
    public SpaceTransferResponse transferBetweenSpaces(Long userId, CreateSpaceTransferRequest req) {
        if (req.sourceSpaceId().equals(req.destinationSpaceId())) {
            throw new BusinessException("O espaço de origem e o de destino devem ser diferentes.");
        }

        FinancialSpace src = spaces.findByIdAndUserId(req.sourceSpaceId(), userId)
                .orElseThrow(() -> new NotFoundException("Espaço de origem não encontrado"));
        FinancialSpace dst = spaces.findByIdAndUserId(req.destinationSpaceId(), userId)
                .orElseThrow(() -> new NotFoundException("Espaço de destino não encontrado"));

        if (!Boolean.TRUE.equals(src.getActive()) || !Boolean.TRUE.equals(dst.getActive())) {
            throw new BusinessException("Ambos os espaços financeiros devem estar ativos.");
        }

        if (!src.getCurrency().equalsIgnoreCase(dst.getCurrency())) {
            throw new BusinessException("Transferência entre espaços exige a mesma moeda base (" + src.getCurrency() + " vs " + dst.getCurrency() + ").");
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        String currency = src.getCurrency();
        LedgerAccount srcEquity = ledger.ensureSystemAccount(
                user,
                "EQUITY:SPACE:" + src.getId() + ":" + currency,
                "Patrimônio Alocado — " + src.getName(),
                LedgerAccountClass.EQUITY,
                currency
        );
        LedgerAccount dstEquity = ledger.ensureSystemAccount(
                user,
                "EQUITY:SPACE:" + dst.getId() + ":" + currency,
                "Patrimônio Alocado — " + dst.getName(),
                LedgerAccountClass.EQUITY,
                currency
        );

        String idempotencyKey = (req.idempotencyKey() != null && !req.idempotencyKey().isBlank())
                ? "SPACE_TRANSFER:" + req.idempotencyKey().trim()
                : "SPACE_TRANSFER:" + UUID.randomUUID();

        JournalEntry entry = null;
        if (srcEquity != null && dstEquity != null) {
            entry = ledger.postEntry(new PostEntryCommand(
                    user,
                    idempotencyKey,
                    req.transferDate(),
                    req.transferDate(),
                    "Transferência entre espaços: " + src.getName() + " -> " + dst.getName() + " (" + req.description().trim() + ")",
                    "SPACE_TRANSFER",
                    null,
                    RecordOrigin.INTERNAL,
                    JournalEntryStatus.POSTED,
                    List.of(
                            new PostingLine(srcEquity.getId(), PostingSide.DEBIT, req.amount(), "Saída de capital do espaço " + src.getName()),
                            new PostingLine(dstEquity.getId(), PostingSide.CREDIT, req.amount(), "Aporte de capital no espaço " + dst.getName())
                    )
            ));
        }

        SpaceTransfer saved = transfers.save(SpaceTransfer.builder()
                .user(user)
                .sourceSpace(src)
                .destinationSpace(dst)
                .amount(req.amount().setScale(2, RoundingMode.HALF_EVEN))
                .currency(currency)
                .transferDate(req.transferDate())
                .description(req.description().trim())
                .journalEntry(entry)
                .build());

        return SpaceTransferResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SpaceTransferResponse> listTransfers(Long userId) {
        return transfers.findTop50ByUserIdOrderByTransferDateDescIdDesc(userId).stream()
                .map(SpaceTransferResponse::from)
                .toList();
    }

    @Transactional
    public void allocateTransaction(Long userId, Long transactionId, Long spaceId) {
        Transaction tx = transactions.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada"));
        FinancialSpace targetSpace = spaces.findByIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new NotFoundException("Espaço financeiro não encontrado"));

        if (!Boolean.TRUE.equals(targetSpace.getActive())) {
            throw new BusinessException("Não é possível alocar transações em um espaço arquivado.");
        }

        tx.setSpace(targetSpace);
        transactions.save(tx);

        if (tx.getTransferPairId() != null) {
            transactions.findByIdAndUserId(tx.getTransferPairId(), userId)
                    .ifPresent(pair -> {
                        pair.setSpace(targetSpace);
                        transactions.save(pair);
                    });
        }
    }

    @Transactional(readOnly = true)
    public SpaceReportResponse getSpaceReport(Long userId, Long spaceId, LocalDate from, LocalDate to) {
        FinancialSpace space = spaces.findByIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new NotFoundException("Espaço financeiro não encontrado"));

        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        if (end.isBefore(start)) {
            throw new BusinessException("A data final deve ser igual ou posterior à data inicial.");
        }

        return buildSpaceReport(userId, space, start, end);
    }

    @Transactional
    public ConsolidatedSpacesReportResponse getConsolidatedReport(Long userId, LocalDate from, LocalDate to) {
        ensureDefaultPersonalSpace(userId);
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        if (end.isBefore(start)) {
            throw new BusinessException("A data final deve ser igual ou posterior à data inicial.");
        }

        List<FinancialSpace> activeSpaces = spaces.findAllByUserIdAndActiveTrueOrderByDefaultSpaceDescNameAsc(userId);
        List<SpaceReportResponse> reports = activeSpaces.stream()
                .map(s -> buildSpaceReport(userId, s, start, end))
                .toList();

        BigDecimal totalIncome = reports.stream()
                .map(SpaceReportResponse::operationalIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = reports.stream()
                .map(SpaceReportResponse::operationalExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOperatingResult = totalIncome.subtract(totalExpense);
        BigDecimal netTransfers = reports.stream()
                .map(r -> r.transfersIn().subtract(r.transfersOut()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal consolidatedMargin = computeProfitMargin(totalIncome, totalOperatingResult);

        return new ConsolidatedSpacesReportResponse(
                start,
                end,
                totalIncome.setScale(2, RoundingMode.HALF_EVEN),
                totalExpense.setScale(2, RoundingMode.HALF_EVEN),
                totalOperatingResult.setScale(2, RoundingMode.HALF_EVEN),
                consolidatedMargin,
                netTransfers.setScale(2, RoundingMode.HALF_EVEN),
                reports
        );
    }

    private SpaceReportResponse buildSpaceReport(Long userId, FinancialSpace space, LocalDate start, LocalDate end) {
        BigDecimal income = analytics.sumBySpaceAndType(
                userId, space.getId(), TransactionType.INCOME, TransactionStatus.POSTED, start, end);
        BigDecimal expense = analytics.sumBySpaceAndType(
                userId, space.getId(), TransactionType.EXPENSE, TransactionStatus.POSTED, start, end);
        BigDecimal operatingResult = income.subtract(expense);
        BigDecimal margin = computeProfitMargin(income, operatingResult);

        BigDecimal transfersIn = transfers.sumIncomingForSpace(userId, space.getId(), start, end);
        BigDecimal transfersOut = transfers.sumOutgoingForSpace(userId, space.getId(), start, end);
        BigDecimal netCashFlow = operatingResult.add(transfersIn).subtract(transfersOut);

        long txCount = analytics.countTransactionsInPeriod(userId, space.getId(), start, end);
        long accCount = analytics.countActiveAccounts(userId, space.getId());
        long goalCount = analytics.countActiveGoals(userId, space.getId());

        return new SpaceReportResponse(
                space.getId(),
                space.getName(),
                space.getType(),
                space.getCurrency(),
                start,
                end,
                income.setScale(2, RoundingMode.HALF_EVEN),
                expense.setScale(2, RoundingMode.HALF_EVEN),
                operatingResult.setScale(2, RoundingMode.HALF_EVEN),
                margin,
                transfersIn.setScale(2, RoundingMode.HALF_EVEN),
                transfersOut.setScale(2, RoundingMode.HALF_EVEN),
                netCashFlow.setScale(2, RoundingMode.HALF_EVEN),
                txCount,
                accCount,
                goalCount
        );
    }

    private BigDecimal computeProfitMargin(BigDecimal income, BigDecimal operatingResult) {
        if (income == null || income.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return operatingResult
                .multiply(new BigDecimal("100"))
                .divide(income, 2, RoundingMode.HALF_EVEN);
    }

    private FinancialAccount resolveDefaultAccount(Long userId, Long accountId) {
        if (accountId == null) {
            return null;
        }
        return accounts.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Conta vinculada ao espaço não encontrada"));
    }

    private String generateUniqueSlug(Long userId, String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "espaco";
        }
        if (base.length() > 65) {
            base = base.substring(0, 65);
        }
        String candidate = base;
        int suffix = 2;
        while (spaces.existsByUserIdAndSlug(userId, candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
