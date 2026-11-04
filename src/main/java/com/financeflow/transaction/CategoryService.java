package com.financeflow.transaction;

import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.transaction.dto.CategoryResponse;
import com.financeflow.transaction.dto.CreateCategoryRequest;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final TransactionCategoryRepository categories;
    private final UserRepository users;

    public CategoryService(TransactionCategoryRepository categories, UserRepository users) {
        this.categories = categories;
        this.users = users;
    }

    @Transactional
    public CategoryResponse create(Long userId, CreateCategoryRequest req) {
        if (categories.existsByUserIdAndNameIgnoreCase(userId, req.name().trim())) {
            throw new BusinessException("Já existe uma categoria com esse nome.");
        }
        var user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        var cat = TransactionCategory.builder()
                .user(user)
                .name(req.name().trim())
                .type(req.type())
                .color(req.color())
                .icon(req.icon())
                .build();
        return CategoryResponse.from(categories.save(cat));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        return categories.findAllByUserIdOrderByNameAsc(userId)
                .stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        var cat = categories.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
        categories.delete(cat);
    }
}