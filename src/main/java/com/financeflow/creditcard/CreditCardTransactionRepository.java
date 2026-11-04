package com.financeflow.creditcard;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreditCardTransactionRepository extends JpaRepository<CreditCardTransaction, Long> {
    List<CreditCardTransaction> findAllByInvoiceIdOrderByTransactionDateDesc(Long invoiceId);
}