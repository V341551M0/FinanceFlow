package com.financeflow.dashboard;

import com.financeflow.common.exception.BusinessException;
import com.financeflow.dashboard.dto.DashboardResponse;
import com.financeflow.security.UserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    public DashboardResponse get(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "THIS_MONTH") PeriodPreset preset,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        DateRange range;
        if (preset == PeriodPreset.CUSTOM) {
            if (from == null || to == null)
                throw new BusinessException("Para preset=CUSTOM, informe from e to.");
            range = new DateRange(from, to);
        } else {
            range = preset.resolve(LocalDate.now());
        }
        return service.build(p.getId(), range);
    }
}