package com.financeflow.statement;

import com.financeflow.security.UserPrincipal;
import com.financeflow.statement.dto.ImportResultResponse;
import com.financeflow.statement.dto.StatementPreviewResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/statements")
public class StatementImportController {

    private final StatementImportService service;

    public StatementImportController(StatementImportService service) { this.service = service; }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StatementPreviewResponse preview(@AuthenticationPrincipal UserPrincipal p,
                                            @RequestParam Long accountId,
                                            @RequestParam("file") MultipartFile file) {
        return service.preview(p.getId(), accountId, file);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultResponse importFile(@AuthenticationPrincipal UserPrincipal p,
                                           @RequestParam Long accountId,
                                           @RequestParam("file") MultipartFile file) {
        return service.importFile(p.getId(), accountId, file);
    }
}