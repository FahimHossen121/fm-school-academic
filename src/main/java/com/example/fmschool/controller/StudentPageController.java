package com.example.fmschool.controller;

import com.example.fmschool.dto.StudentForm;
import com.example.fmschool.entity.Student;
import com.example.fmschool.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StudentPageController {

    private final StudentService studentService;

    public StudentPageController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/students")
    public String showStudents(Model model) {
        return "redirect:/admin/students";
    }

    @GetMapping("/students/new")
    public String showCreateForm(Model model){
        return "redirect:/admin/students/new";
    }

    @PostMapping("/students")
    public String createStudent(
            @Valid @ModelAttribute("studentForm") StudentForm form, BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()){
            return "student-form";
        }
        studentService.createStudent(form);
        return "redirect:/students";
    }

    @GetMapping("/students/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Student student = studentService.getStudentById(id);
        StudentForm form = new StudentForm();
        form.setName(student.getName());
        form.setRoll(student.getRoll());

        model.addAttribute("studentForm", form);
        model.addAttribute("studentId", id);
        return "student-edit";
    }

    @PostMapping("/students/{id}")
    public String updateStudent(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("studentForm") StudentForm form,
            BindingResult bindingResult,
            Model model) {
        studentService.getStudentById(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("studentId", id);
            return "student-edit";
        }

        studentService.updateStudent(id, form);
        return "redirect:/students";
    }

    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable("id") Long id) {
        studentService.deleteStudent(id);
        return "redirect:/students";
    }
}
