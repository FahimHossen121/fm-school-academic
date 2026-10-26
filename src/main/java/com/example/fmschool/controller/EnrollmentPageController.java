package com.example.fmschool.controller;

import com.example.fmschool.service.EnrollmentService;
import com.example.fmschool.dto.EnrolledStudentForm;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EnrollmentPageController {
    private final EnrollmentService enrollments;

    public EnrollmentPageController(EnrollmentService enrollments) {
        this.enrollments = enrollments;
    }

    @GetMapping({"/enrollments", "/admin/enrollments"})
    public String list(Model model) {
        var board = enrollments.getBoard();
        model.addAttribute("board", board);
        model.addAttribute("isAdmin", board.role() == com.example.fmschool.entity.Role.ADMIN);
        model.addAttribute("isTeacher", board.role() == com.example.fmschool.entity.Role.TEACHER);
        model.addAttribute("isStudent", board.role() == com.example.fmschool.entity.Role.STUDENT);
        return "enrollments";
    }

    @PostMapping("/enrollments/apply")
    public String apply(@RequestParam("teacherId") Long teacherId, RedirectAttributes flash) {
        return perform(() -> enrollments.apply(teacherId), "Application sent. Wait for the Teacher to respond.", flash);
    }

    @PostMapping("/enrollments/invite")
    public String invite(@RequestParam("studentId") Long studentId, RedirectAttributes flash) {
        return perform(() -> enrollments.invite(studentId), "Invitation sent. Wait for the Student to respond.", flash);
    }

    @PostMapping("/admin/enrollments")
    public String associate(@RequestParam("teacherId") Long teacherId,
                            @RequestParam("studentId") Long studentId, RedirectAttributes flash) {
        return perform(() -> enrollments.associate(teacherId, studentId), "Association created by Admin.", flash);
    }

    @PostMapping("/enrollments/{id}/accept")
    public String accept(@PathVariable("id") Long id, RedirectAttributes flash) {
        return perform(() -> enrollments.respond(id, true), "Request accepted.", flash);
    }

    @PostMapping("/enrollments/{id}/reject")
    public String reject(@PathVariable("id") Long id, RedirectAttributes flash) {
        return perform(() -> enrollments.respond(id, false), "Request rejected.", flash);
    }

    @PostMapping("/enrollments/{id}/remove")
    public String remove(@PathVariable("id") Long id, RedirectAttributes flash) {
        return perform(() -> enrollments.remove(id), "Association removed. Both accounts remain.", flash);
    }

    @GetMapping("/enrollments/{id}/student/edit")
    public String editStudent(@PathVariable("id") Long id, Model model) {
        var view = enrollments.getStudentForEdit(id);
        model.addAttribute("studentForm", view.form());
        model.addAttribute("edit", view);
        return "enrolled-student-edit";
    }

    @PostMapping("/enrollments/{id}/student")
    public String saveStudent(@PathVariable("id") Long id,
                              @Valid @ModelAttribute("studentForm") EnrolledStudentForm form,
                              BindingResult errors, Model model, RedirectAttributes flash) {
        // Recheck authority even if an old edit form was left open.
        var view = enrollments.getStudentForEdit(id);
        if (errors.hasErrors()) {
            model.addAttribute("edit", view);
            return "enrolled-student-edit";
        }
        enrollments.updateEnrolledStudent(id, form);
        flash.addFlashAttribute("message", "Student information and marks saved. Login credentials are unchanged.");
        return "redirect:/enrollments";
    }

    private String perform(Runnable operation, String message, RedirectAttributes flash) {
        try {
            operation.run();
            flash.addFlashAttribute("message", message);
        } catch (ResponseStatusException ex) {
            if (!ex.getStatusCode().equals(HttpStatus.CONFLICT)) {
                throw ex;
            }
            flash.addFlashAttribute("error", ex.getReason());
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "The request changed while saving. Refresh and check its current status.");
        }
        return "redirect:/enrollments";
    }
}
