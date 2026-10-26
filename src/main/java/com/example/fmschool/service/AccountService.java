package com.example.fmschool.service;

import com.example.fmschool.dto.AccountForm;
import com.example.fmschool.entity.Account;
import com.example.fmschool.entity.Role;
import com.example.fmschool.repository.AccountRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AccountService {
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;

    public AccountService(AccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    public record Summary(Long id, String username, Role role) {}

    public List<Summary> getAccounts() {
        return accounts.findAll().stream()
                .map(a -> new Summary(a.getId(), a.getUsername(), a.getRole())).toList();
    }

    @Transactional
    public Long createAccount(AccountForm form) {
        if (form.getRole() != Role.STUDENT && form.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Only Teacher and Student accounts can be created");
        }
        if (accounts.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes");
        }

        Account account = new Account();
        account.setUsername(form.getUsername());
        account.setPasswordHash(encoder.encode(form.getPassword()));
        account.setRole(form.getRole());
        return accounts.save(account).getId();
    }
}
