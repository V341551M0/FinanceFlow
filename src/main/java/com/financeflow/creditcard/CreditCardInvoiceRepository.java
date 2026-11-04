package com.financeflow.creditcard;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CreditCardInvoiceRepository extends JpaRepository<CreditCardInvoice, Long> {
    Optional<CreditCardInvoice> findByCardIdAndReferenceMonth(Long cardId, LocalDate referenceMonth);
    List<CreditCardInvoice> findAllByCardIdOrderByReferenceMonthDesc(Long cardId);
    Optional<CreditCardInvoice> findByIdAndCardUserId(Long id, Long userId);
}