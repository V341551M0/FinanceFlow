package com.financeflow.ledger;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);
    Optional<JournalEntry> findByEntryUuidAndUserId(UUID entryUuid, Long userId);
    Optional<JournalEntry> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @EntityGraph(attributePaths = {"postings", "postings.ledgerAccount"})
    List<JournalEntry> findTop100ByUserIdOrderByEntryDateDescIdDesc(Long userId);
}
