package com.financeflow.budget;

import com.financeflow.budget.dto.BudgetResponse;
import com.financeflow.budget.dto.CreateBudgetRequest;
import com.financeflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) { this.service = service; }

    @GetMapping
    public List<BudgetResponse> list(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return service.list(p.getId(), month != null ? month : LocalDate.now());
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@AuthenticationPrincipal UserPrincipal p,
                                                 @Valid @RequestBody CreateBudgetRequest req) {
        return ResponseEntity.status(201).body(service.create(p.getId(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal p,
                                       @PathVariable Long id) {
        service.delete(p.getId(), id);
        return ResponseEntity.noContent().build();
    }
}