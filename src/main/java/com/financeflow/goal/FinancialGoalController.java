package com.financeflow.goal;

import com.financeflow.goal.dto.*;
import com.financeflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class FinancialGoalController {

    private final FinancialGoalService service;

    public FinancialGoalController(FinancialGoalService service) { this.service = service; }

    @GetMapping
    public List<GoalResponse> list(@AuthenticationPrincipal UserPrincipal p) {
        return service.list(p.getId());
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@AuthenticationPrincipal UserPrincipal p,
                                               @Valid @RequestBody CreateGoalRequest req) {
        return ResponseEntity.status(201).body(service.create(p.getId(), req));
    }

    @PostMapping("/{id}/contribute")
    public GoalResponse contribute(@AuthenticationPrincipal UserPrincipal p,
                                   @PathVariable Long id,
                                   @Valid @RequestBody ContributeRequest req) {
        return service.contribute(p.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal p,
                                       @PathVariable Long id) {
        service.delete(p.getId(), id);
        return ResponseEntity.noContent().build();
    }
}