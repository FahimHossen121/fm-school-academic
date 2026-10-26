package com.example.fmschool.controller;

import com.example.fmschool.dto.SignupForm;
import com.example.fmschool.service.SignupService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class SignupController {
    private final SignupService signup;

    public SignupController(SignupService signup) { this.signup = signup; }

    @InitBinder("signupForm")
    public void allowedFields(WebDataBinder binder) {
        binder.setAllowedFields("username", "password", "name", "roll", "course");
    }

    @GetMapping("/signup")
    public String choose(Authentication authentication) {
        return signedIn(authentication) ? "redirect:/" : "signup-choice";
    }

    @GetMapping("/signup/student")
    public String studentForm(Authentication authentication, Model model) {
        return form(authentication, model, true);
    }

    @GetMapping("/signup/teacher")
    public String teacherForm(Authentication authentication, Model model) {
        return form(authentication, model, false);
    }

    @PostMapping("/signup/student")
    public String student(@Valid @ModelAttribute("signupForm") SignupForm form,
                          BindingResult errors, Model model) {
        return register(form, errors, model, true);
    }

    @PostMapping("/signup/teacher")
    public String teacher(@Valid @ModelAttribute("signupForm") SignupForm form,
                          BindingResult errors, Model model) {
        return register(form, errors, model, false);
    }

    private String form(Authentication authentication, Model model, boolean student) {
        if (signedIn(authentication)) {
            return "redirect:/";
        }
        model.addAttribute("signupForm", new SignupForm());
        details(model, student);
        return "signup";
    }

    private String register(SignupForm form, BindingResult errors, Model model, boolean student) {
        if (student && (form.getRoll() == null || form.getRoll().isBlank())) {
            errors.rejectValue("roll", "required", "Roll is required");
        }
        if (!student && (form.getCourse() == null || form.getCourse().isBlank())) {
            errors.rejectValue("course", "required", "Course is required");
        }
        if (!errors.hasErrors()) {
            try {
                if (student) {
                    signup.registerStudent(form);
                } else {
                    signup.registerTeacher(form);
                }
                return "redirect:/login?registered";
            } catch (IllegalArgumentException ex) {
                errors.reject("signup", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("signup", "Username is already taken. Choose another username.");
            }
        }
        form.setPassword(null);
        details(model, student);
        return "signup";
    }

    private void details(Model model, boolean student) {
        model.addAttribute("isStudent", student);
        model.addAttribute("signupPath", student ? "/signup/student" : "/signup/teacher");
    }

    private boolean signedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
