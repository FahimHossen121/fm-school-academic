package com.example.fmschool.controller;

import com.example.fmschool.dto.ProfileForm;
import com.example.fmschool.entity.Role;
import com.example.fmschool.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProfilePageController {
    private final ProfileService profiles;

    public ProfilePageController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/me")
    public String ownProfile(Model model) {
        return show(profiles.getOwnProfile(), false, model);
    }

    @PostMapping("/me")
    public String saveOwnProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                 BindingResult errors, Model model) {
        var view = profiles.getOwnProfile();
        return save(view, false, form, errors, model);
    }

    @PostMapping("/me/delete")
    public String deleteOwnAccount(HttpServletRequest request, HttpServletResponse response,
                                   Authentication authentication) {
        profiles.deleteOwnAccount();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?deleted";
    }

    @GetMapping("/admin/accounts/{id}/profile")
    public String managedProfile(@PathVariable("id") Long id, Model model) {
        return show(profiles.getManagedProfile(id), true, model);
    }

    @PostMapping("/admin/accounts/{id}/profile")
    public String saveManagedProfile(@PathVariable("id") Long id,
                                     @Valid @ModelAttribute("profileForm") ProfileForm form,
                                     BindingResult errors, Model model) {
        return save(profiles.getManagedProfile(id), true, form, errors, model);
    }

    @PostMapping("/admin/accounts/{id}/delete")
    public String deleteManagedAccount(@PathVariable("id") Long id) {
        Role role = profiles.getManagedProfile(id).role();
        profiles.deleteManagedAccount(id);
        return "redirect:" + managementPath(role);
    }

    private String show(ProfileService.View view, boolean admin, Model model) {
        model.addAttribute("profileForm", view.form());
        details(view, admin, model);
        return "profile";
    }

    private String save(ProfileService.View view, boolean admin, ProfileForm form,
                        BindingResult errors, Model model) {
        if (view.role() == Role.STUDENT && (form.getRoll() == null || form.getRoll().isBlank())) {
            errors.rejectValue("roll", "required", "Roll is required");
        }
        if (view.role() == Role.TEACHER && (form.getCourse() == null || form.getCourse().isBlank())) {
            errors.rejectValue("course", "required", "Course is required");
        }
        if (!errors.hasErrors()) {
            try {
                if (admin) {
                    profiles.updateManagedProfile(view.accountId(), form);
                } else {
                    profiles.updateOwnProfile(form);
                }
                return admin ? "redirect:" + managementPath(view.role()) + "?saved" : "redirect:/me?saved";
            } catch (IllegalArgumentException ex) {
                errors.reject("profile", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("profile", "This profile is already linked. Reload and try again.");
            }
        }
        details(view, admin, model);
        return "profile";
    }

    private void details(ProfileService.View view, boolean admin, Model model) {
        model.addAttribute("profile", view);
        model.addAttribute("isStudent", view.role() == Role.STUDENT);
        model.addAttribute("adminMode", admin);
        model.addAttribute("savePath", admin ? "/admin/accounts/" + view.accountId() + "/profile" : "/me");
        model.addAttribute("deletePath", admin ? "/admin/accounts/" + view.accountId() + "/delete" : "/me/delete");
        model.addAttribute("managementPath", managementPath(view.role()));
    }

    private String managementPath(Role role) {
        return role == Role.STUDENT ? "/admin/students" : "/admin/teachers";
    }
}
