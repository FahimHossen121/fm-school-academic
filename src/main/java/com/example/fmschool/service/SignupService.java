package com.example.fmschool.service;

import com.example.fmschool.dto.SignupForm;
import com.example.fmschool.entity.*;
import com.example.fmschool.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.nio.charset.StandardCharsets;

@Service
@Validated
@PreAuthorize("isAnonymous()")
public class SignupService {
    private final AccountRepository accounts;
    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final PasswordEncoder encoder;

    public SignupService(AccountRepository accounts, StudentRepository students,
                         TeacherRepository teachers, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.students = students;
        this.teachers = teachers;
        this.encoder = encoder;
    }

    @Transactional
    public void registerStudent(@Valid SignupForm form) {
        if (form.getRoll() == null || form.getRoll().isBlank()) {
            throw new IllegalArgumentException("Roll is required");
        }
        Account account = createIdentity(form, Role.STUDENT);
        Student student = new Student();
        student.setAccount(account);
        student.setName(form.getName());
        student.setRoll(form.getRoll());
        students.save(student);
    }

    @Transactional
    public void registerTeacher(@Valid SignupForm form) {
        if (form.getCourse() == null || form.getCourse().isBlank()) {
            throw new IllegalArgumentException("Course is required");
        }
        Account account = createIdentity(form, Role.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setAccount(account);
        teacher.setName(form.getName());
        teacher.setCourse(form.getCourse());
        teachers.save(teacher);
    }

    private Account createIdentity(SignupForm form, Role role) {
        if (accounts.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes");
        }
        Account account = new Account();
        account.setUsername(form.getUsername());
        account.setPasswordHash(encoder.encode(form.getPassword()));
        // Role is selected by the server endpoint, never bound from submitted input.
        account.setRole(role);
        return accounts.save(account);
    }
}
