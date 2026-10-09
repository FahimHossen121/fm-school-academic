package com.example.fmschool.controller;

import com.example.fmschool.dto.StudentForm;
import com.example.fmschool.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class StudentPageController {

    private final StudentService studentService;

    public StudentPageController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/students")
    public String showStudents(Model model) {
        model.addAttribute("students", studentService.getAllStudents());
        return "students";
    }

    @GetMapping("/students/new")
    public String showCreateForm(Model model){
        model.addAttribute("studentForm", new StudentForm());
        return "student-form";
    }

    @PostMapping("/students")
    public String createStudent(
            @ModelAttribute("studentForm") StudentForm form
    ) {
        studentService.createStudent(form);
        return "redirect:/students";
    }
}
