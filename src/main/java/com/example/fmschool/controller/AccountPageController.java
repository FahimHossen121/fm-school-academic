package com.example.fmschool.controller;

import com.example.fmschool.dto.AccountForm;
import com.example.fmschool.entity.Role;
import com.example.fmschool.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Controller
public class AccountPageController {
    private final AccountService accounts;

    public AccountPageController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("isAdmin", authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN")));
        model.addAttribute("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull).filter(a -> a.startsWith("ROLE_")).toList());
        return "home";
    }

    @GetMapping("/admin/accounts")
    public String listAccounts(Model model) {
        return "redirect:/";
    }

    @ModelAttribute("availableRoles")
    public Role[] availableRoles() {
        return new Role[]{Role.TEACHER, Role.STUDENT};
    }

    @GetMapping("/admin/accounts/new")
    public String newAccount(Model model) {
        return "redirect:/admin/students/new";
    }

    @PostMapping("/admin/accounts")
    public String createAccount(@Valid @ModelAttribute("accountForm") AccountForm form,
                                BindingResult errors) {
        if (errors.hasErrors()) {
            form.setPassword(null);
            return "account-form";
        }
        try {
            accounts.createAccount(form);
        } catch (IllegalArgumentException ex) {
            errors.reject("account", ex.getMessage());
            form.setPassword(null);
            return "account-form";
        } catch (DataIntegrityViolationException ex) {
            errors.reject("account", "Username is already taken");
            form.setPassword(null);
            return "account-form";
        }
        return "redirect:/admin/accounts";
    }
}
