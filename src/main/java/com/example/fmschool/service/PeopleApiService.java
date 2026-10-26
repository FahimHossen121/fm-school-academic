package com.example.fmschool.service;

import com.example.fmschool.dto.*;
import com.example.fmschool.entity.*;
import com.example.fmschool.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public class PeopleApiService {
    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final AccountRepository accounts;
    private final AccountService accountService;
    private final ProfileService profiles;
    private final StudentService studentService;

    public PeopleApiService(StudentRepository students, TeacherRepository teachers,
                            AccountRepository accounts, AccountService accountService,
                            ProfileService profiles, StudentService studentService) {
        this.students = students;
        this.teachers = teachers;
        this.accounts = accounts;
        this.accountService = accountService;
        this.profiles = profiles;
        this.studentService = studentService;
    }

    public record StudentView(Long id, Long accountId, String username, String name, String roll) {}
    public record TeacherView(Long id, Long accountId, String username, String name, String course) {}

    @Transactional(readOnly = true)
    public List<StudentView> students() {
        return students.findAll().stream().map(this::studentView).toList();
    }

    @Transactional(readOnly = true)
    public StudentView student(Long id) {
        return studentView(studentEntity(id));
    }

    public StudentView createStudent(CreatePersonRequest request) {
        Long accountId = createIdentity(request, Role.STUDENT);
        profiles.updateManagedProfile(accountId, request.profile());
        return studentView(students.findByAccountId(accountId).orElseThrow());
    }

    public StudentView updateStudent(Long id, StudentForm form) {
        studentService.updateStudent(id, form);
        return studentView(studentEntity(id));
    }

    public void deleteStudent(Long id) {
        Student student = studentEntity(id);
        if (student.getAccount() != null) {
            profiles.deleteManagedAccount(student.getAccount().getId());
        } else {
            students.delete(student);
        }
    }

    @Transactional(readOnly = true)
    public List<TeacherView> teachers() {
        return teachers.findAll().stream().map(this::teacherView).toList();
    }

    @Transactional(readOnly = true)
    public TeacherView teacher(Long id) {
        return teacherView(teacherEntity(id));
    }

    public TeacherView createTeacher(CreatePersonRequest request) {
        Long accountId = createIdentity(request, Role.TEACHER);
        profiles.updateManagedProfile(accountId, request.profile());
        return teacherView(teachers.findByAccountId(accountId).orElseThrow());
    }

    public TeacherView updateTeacher(Long id, ProfileForm form) {
        Teacher teacher = teacherEntity(id);
        profiles.updateManagedProfile(teacher.getAccount().getId(), form);
        return teacherView(teacher);
    }

    public void deleteTeacher(Long id) {
        profiles.deleteManagedAccount(teacherEntity(id).getAccount().getId());
    }

    private Long createIdentity(CreatePersonRequest request, Role role) {
        if (accounts.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        AccountForm form = new AccountForm();
        form.setUsername(request.username());
        form.setPassword(request.password());
        form.setRole(role);
        return accountService.createAccount(form);
    }

    private Student studentEntity(Long id) {
        return students.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    private Teacher teacherEntity(Long id) {
        return teachers.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
    }

    private StudentView studentView(Student student) {
        Account account = student.getAccount();
        return new StudentView(student.getId(), account == null ? null : account.getId(),
                account == null ? null : account.getUsername(), student.getName(), student.getRoll());
    }

    private TeacherView teacherView(Teacher teacher) {
        return new TeacherView(teacher.getId(), teacher.getAccount().getId(),
                teacher.getAccount().getUsername(), teacher.getName(), teacher.getCourse());
    }
}
