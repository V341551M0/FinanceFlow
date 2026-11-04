package com.financeflow.creditcard;

import com.financeflow.creditcard.dto.*;
import com.financeflow.security.UserPrincipal;
import com.financeflow.transaction.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit-cards")
public class CreditCardController {

    private final CreditCardService service;
    private final TransactionService txService;

    public CreditCardController(CreditCardService service, TransactionService txService) {
        this.service = service;
        this.txService = txService;
    }

    @GetMapping
    public List<CardResponse> list(@AuthenticationPrincipal UserPrincipal p) {
        return service.list(p.getId());
    }

    @PostMapping
    public ResponseEntity<CardResponse> create(@AuthenticationPrincipal UserPrincipal p,
                                               @Valid @RequestBody CreateCardRequest req) {
        return ResponseEntity.status(201).body(service.create(p.getId(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal p,
                                       @PathVariable Long id) {
        service.deactivate(p.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/invoices")
    public List<InvoiceResponse> invoices(@AuthenticationPrincipal UserPrincipal p,
                                          @PathVariable Long id) {
        return service.listInvoices(p.getId(), id);
    }

    @PostMapping("/{id}/purchases")
    public ResponseEntity<Void> addPurchase(@AuthenticationPrincipal UserPrincipal p,
                                            @PathVariable Long id,
                                            @Valid @RequestBody AddPurchaseRequest req) {
        service.addPurchase(p.getId(), id, req);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    public ResponseEntity<Void> payInvoice(@AuthenticationPrincipal UserPrincipal p,
                                           @PathVariable Long invoiceId,
                                           @RequestParam Long accountId) {
        service.payInvoice(p.getId(), invoiceId, accountId, txService);
        return ResponseEntity.ok().build();
    }
}