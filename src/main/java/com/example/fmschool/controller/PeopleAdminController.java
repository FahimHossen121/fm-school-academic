package com.example.fmschool.controller;

import com.example.fmschool.dto.AccountForm;
import com.example.fmschool.entity.Role;
import com.example.fmschool.service.AccountService;
import com.example.fmschool.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class PeopleAdminController {
    private final ProfileService profiles;
    private final AccountService accounts;

    public PeopleAdminController(ProfileService profiles, AccountService accounts) {
        this.profiles = profiles;
        this.accounts = accounts;
    }

    @GetMapping("/admin/teachers")
    public String teachers(Model model) {
        return list(Role.TEACHER, model);
    }

    @GetMapping("/admin/students")
    public String students(Model model) {
        return list(Role.STUDENT, model);
    }

    @GetMapping("/admin/teachers/new")
    public String newTeacher(Model model) {
        return newPerson(Role.TEACHER, model);
    }

    @GetMapping("/admin/students/new")
    public String newStudent(Model model) {
        return newPerson(Role.STUDENT, model);
    }

    @PostMapping("/admin/teachers")
    public String createTeacher(@Valid @ModelAttribute("accountForm") AccountForm form,
                                BindingResult errors, Model model) {
        return create(Role.TEACHER, form, errors, model);
    }

    @PostMapping("/admin/students")
    public String createStudent(@Valid @ModelAttribute("accountForm") AccountForm form,
                                BindingResult errors, Model model) {
        return create(Role.STUDENT, form, errors, model);
    }

    private String list(Role role, Model model) {
        model.addAttribute("people", profiles.listPeople(role));
        model.addAttribute("isStudent", role == Role.STUDENT);
        model.addAttribute("title", role == Role.STUDENT ? "Manage Students" : "Manage Teachers");
        model.addAttribute("newPath", path(role) + "/new");
        return "people";
    }

    private String newPerson(Role role, Model model) {
        AccountForm form = new AccountForm();
        form.setRole(role);
        model.addAttribute("accountForm", form);
        setup(role, model);
        return "person-new";
    }

    private String create(Role role, AccountForm form, BindingResult errors, Model model) {
        // The route chooses the role. A modified hidden field cannot grant ADMIN.
        form.setRole(role);
        if (!errors.hasErrors()) {
            try {
                Long accountId = accounts.createAccount(form);
                return "redirect:/admin/accounts/" + accountId + "/profile";
            } catch (IllegalArgumentException ex) {
                errors.reject("person", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("person", "Username is already taken");
            }
        }
        form.setPassword(null);
        setup(role, model);
        return "person-new";
    }

    private void setup(Role role, Model model) {
        model.addAttribute("title", role == Role.STUDENT ? "Add Student" : "Add Teacher");
        model.addAttribute("savePath", path(role));
    }

    private String path(Role role) {
        return role == Role.STUDENT ? "/admin/students" : "/admin/teachers";
    }
}
