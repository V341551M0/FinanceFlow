package com.financeflow.account;

import com.financeflow.account.dto.AccountResponse;
import com.financeflow.account.dto.CreateAccountRequest;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.user.User;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final FinancialAccountRepository accounts;
    private final UserRepository users;

    public AccountService(FinancialAccountRepository accounts, UserRepository users) {
        this.accounts = accounts;
        this.users = users;
    }

    @Transactional
    public AccountResponse create(Long userId, CreateAccountRequest req) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        var account = FinancialAccount.builder()
                .user(user)
                .name(req.name().trim())
                .institution(req.institution().trim())
                .type(req.type())
                .currency(req.currency().toUpperCase())
                .initialBalance(req.initialBalance())
                .currentBalance(req.initialBalance())
                .active(true)
                .build();

        return AccountResponse.from(accounts.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(Long userId) {
        return accounts.findAllByUserIdAndActiveTrueOrderByNameAsc(userId)
                .stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long userId, Long id) {
        return accounts.findByIdAndUserId(id, userId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
    }

    @Transactional
    public void deactivate(Long userId, Long id) {
        var account = accounts.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        account.setActive(false);
    }
}