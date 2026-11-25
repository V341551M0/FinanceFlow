package com.financeflow.space;

import com.financeflow.security.UserPrincipal;
import com.financeflow.space.dto.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/spaces")
public class FinancialSpaceController {

    private final FinancialSpaceService service;

    public FinancialSpaceController(FinancialSpaceService service) {
        this.service = service;
    }

    @GetMapping
    public List<SpaceResponse> list(@AuthenticationPrincipal UserPrincipal p) {
        return service.list(p.getId());
    }

    @PostMapping
    public ResponseEntity<SpaceResponse> create(@AuthenticationPrincipal UserPrincipal p,
                                                @Valid @RequestBody CreateSpaceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(p.getId(), req));
    }

    @GetMapping("/{id}")
    public SpaceResponse get(@AuthenticationPrincipal UserPrincipal p,
                             @PathVariable Long id) {
        return service.get(p.getId(), id);
    }

    @PutMapping("/{id}")
    public SpaceResponse update(@AuthenticationPrincipal UserPrincipal p,
                                @PathVariable Long id,
                                @Valid @RequestBody UpdateSpaceRequest req) {
        return service.update(p.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@AuthenticationPrincipal UserPrincipal p,
                                        @PathVariable Long id) {
        service.archive(p.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/report")
    public SpaceReportResponse getSpaceReport(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getSpaceReport(p.getId(), id, from, to);
    }

    @GetMapping("/report/consolidated")
    public ConsolidatedSpacesReportResponse getConsolidatedReport(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getConsolidatedReport(p.getId(), from, to);
    }

    @GetMapping("/transfers")
    public List<SpaceTransferResponse> listTransfers(@AuthenticationPrincipal UserPrincipal p) {
        return service.listTransfers(p.getId());
    }

    @PostMapping("/transfers")
    public ResponseEntity<SpaceTransferResponse> transfer(@AuthenticationPrincipal UserPrincipal p,
                                                          @Valid @RequestBody CreateSpaceTransferRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.transferBetweenSpaces(p.getId(), req));
    }

    @PostMapping("/transactions/{transactionId}/allocate")
    public ResponseEntity<Void> allocateTransaction(@AuthenticationPrincipal UserPrincipal p,
                                                    @PathVariable Long transactionId,
                                                    @Valid @RequestBody AllocateTransactionRequest req) {
        service.allocateTransaction(p.getId(), transactionId, req.spaceId());
        return ResponseEntity.noContent().build();
    }
}
