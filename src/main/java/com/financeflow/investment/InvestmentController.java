package com.financeflow.investment;

import com.financeflow.investment.dto.*;
import com.financeflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final PortfolioService service;

    public InvestmentController(PortfolioService service) { this.service = service; }

    @GetMapping
    public List<PositionResponse> list(@AuthenticationPrincipal UserPrincipal p) {
        return service.listPositions(p.getId());
    }

    @PostMapping
    public ResponseEntity<PositionResponse> create(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateInvestmentRequest req) {
        return ResponseEntity.status(201).body(service.create(p.getId(), req));
    }

    @PostMapping("/{id}/operations")
    public PositionResponse addOperation(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id,
            @Valid @RequestBody AddOperationRequest req) {
        return service.addOperation(p.getId(), id, req);
    }

    @GetMapping("/{id}/operations")
    public List<InvestmentOperation> operations(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id) {
        return service.listOperations(p.getId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal p,
                                       @PathVariable Long id) {
        service.delete(p.getId(), id);
        return ResponseEntity.noContent().build();
    }
}