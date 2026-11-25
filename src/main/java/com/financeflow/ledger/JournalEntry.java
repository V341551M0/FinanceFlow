package com.financeflow.ledger;

import com.financeflow.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_uuid", nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID entryUuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "idempotency_key", nullable = false, length = 120, updatable = false)
    private String idempotencyKey;

    @Column(name = "entry_date", nullable = false, updatable = false)
    private LocalDate entryDate;

    @Column(name = "effective_date", nullable = false, updatable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false, length = 255, updatable = false)
    private String description;

    @Column(name = "reference_type", nullable = false, length = 40, updatable = false)
    private String referenceType;

    @Column(name = "reference_id", updatable = false)
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_origin", nullable = false, length = 30, updatable = false)
    @Builder.Default
    private RecordOrigin recordOrigin = RecordOrigin.INTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JournalEntryStatus status = JournalEntryStatus.POSTED;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id", unique = true, updatable = false)
    private JournalEntry reversalOf;

    @Column(name = "metadata_json", updatable = false)
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LedgerPosting> postings = new ArrayList<>();

    public void addPosting(LedgerPosting posting) {
        posting.setJournalEntry(this);
        this.postings.add(posting);
    }

    @PreRemove
    private void preventRemove() {
        throw new IllegalStateException("Lançamentos contábeis não podem ser removidos");
    }
}
