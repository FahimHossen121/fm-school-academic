package com.example.fmschool.service;

import com.example.fmschool.dto.ProfileForm;
import com.example.fmschool.entity.*;
import com.example.fmschool.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("isAuthenticated()")
public class ProfileService {
    private final AccountRepository accounts;
    private final StudentRepository students;
    private final TeacherRepository teachers;

    public ProfileService(AccountRepository accounts, StudentRepository students,
                          TeacherRepository teachers) {
        this.accounts = accounts;
        this.students = students;
        this.teachers = teachers;
    }

    public record View(Long accountId, String username, Role role, Long recordId, ProfileForm form) {}

    public record ManagementRow(Long accountId, Long recordId, String username,
                                String name, String detail) {}

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.List<ManagementRow> listPeople(Role role) {
        if (role != Role.STUDENT && role != Role.TEACHER) {
            throw new IllegalArgumentException("Choose Students or Teachers");
        }
        var rows = new java.util.ArrayList<ManagementRow>();
        for (Account account : accounts.findByRoleOrderByUsernameAsc(role)) {
            View profile = view(account);
            rows.add(new ManagementRow(account.getId(), profile.recordId(), account.getUsername(),
                    profile.form().getName(),
                    role == Role.STUDENT ? profile.form().getRoll() : profile.form().getCourse()));
        }
        if (role == Role.STUDENT) {
            for (Student student : students.findAll()) {
                if (student.getAccount() == null) {
                    rows.add(new ManagementRow(null, student.getId(), null,
                            student.getName(), student.getRoll()));
                }
            }
        }
        return rows;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public View getOwnProfile() {
        return view(currentAccount());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public void updateOwnProfile(ProfileForm form) {
        // Identity always comes from the logged-in session, never a submitted ID.
        save(currentAccount(), form);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public void deleteOwnAccount() {
        delete(currentAccount());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public View getManagedProfile(Long accountId) {
        return view(managedAccount(accountId));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateManagedProfile(Long accountId, ProfileForm form) {
        save(managedAccount(accountId), form);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteManagedAccount(Long accountId) {
        delete(managedAccount(accountId));
    }

    private Account currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accounts.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
    }

    private Account managedAccount(Long id) {
        Account account = accounts.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        if (account.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The single Admin account is protected");
        }
        return account;
    }

    private View view(Account account) {
        ProfileForm form = new ProfileForm();
        Long recordId = null;
        if (account.getRole() == Role.STUDENT) {
            var student = students.findByAccountId(account.getId());
            if (student.isPresent()) {
                form.setName(student.get().getName());
                form.setRoll(student.get().getRoll());
                recordId = student.get().getId();
            }
        } else if (account.getRole() == Role.TEACHER) {
            var teacher = teachers.findByAccountId(account.getId());
            if (teacher.isPresent()) {
                form.setName(teacher.get().getName());
                form.setCourse(teacher.get().getCourse());
                recordId = teacher.get().getId();
            }
        }
        return new View(account.getId(), account.getUsername(), account.getRole(), recordId, form);
    }

    private void save(Account account, ProfileForm form) {
        if (account.getRole() == Role.STUDENT) {
            if (form.getRoll() == null || form.getRoll().isBlank()) {
                throw new IllegalArgumentException("Roll is required");
            }
            Student student = students.findByAccountId(account.getId()).orElse(null);
            if (student == null) {
                student = new Student();
            }
            student.setAccount(account);
            student.setName(form.getName());
            student.setRoll(form.getRoll());
            students.save(student);
        } else if (account.getRole() == Role.TEACHER) {
            if (form.getCourse() == null || form.getCourse().isBlank()) {
                throw new IllegalArgumentException("Course is required");
            }
            Teacher teacher = teachers.findByAccountId(account.getId()).orElseGet(Teacher::new);
            teacher.setAccount(account);
            teacher.setName(form.getName());
            teacher.setCourse(form.getCourse());
            teachers.save(teacher);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void delete(Account account) {
        students.findByAccountId(account.getId()).ifPresent(students::delete);
        teachers.findByAccountId(account.getId()).ifPresent(teachers::delete);
        // Remove dependent records before the account, preserving unrelated people.
        students.flush();
        teachers.flush();
        accounts.delete(account);
    }
}
