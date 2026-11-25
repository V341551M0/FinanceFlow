package com.financeflow.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;

public interface LedgerPostingRepository extends JpaRepository<LedgerPosting, Long> {

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM LedgerPosting p
        WHERE p.ledgerAccount.id = :accountId
          AND p.side = :side
          AND p.journalEntry.status IN :statuses
    """)
    BigDecimal sumByAccountAndSideAndStatuses(
            @Param("accountId") Long accountId,
            @Param("side") PostingSide side,
            @Param("statuses") Collection<JournalEntryStatus> statuses
    );
}
