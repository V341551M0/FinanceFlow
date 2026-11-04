package com.financeflow.transaction;

import com.financeflow.security.UserPrincipal;
import com.financeflow.transaction.dto.CategoryResponse;
import com.financeflow.transaction.dto.CreateCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal UserPrincipal p) {
        return service.list(p.getId());
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateCategoryRequest req) {
        return ResponseEntity.status(201).body(service.create(p.getId(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal p,
                                       @PathVariable Long id) {
        service.delete(p.getId(), id);
        return ResponseEntity.noContent().build();
    }
}