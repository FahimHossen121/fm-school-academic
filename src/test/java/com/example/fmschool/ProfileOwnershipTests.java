package com.example.fmschool;

import com.example.fmschool.dto.ProfileForm;
import com.example.fmschool.entity.*;
import com.example.fmschool.repository.*;
import com.example.fmschool.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ProfileOwnershipTests.Config.class)
class ProfileOwnershipTests {
    @Configuration
    @EnableMethodSecurity
    @Import(ProfileService.class)
    static class Config {
        @Bean AccountRepository accounts() { return mock(AccountRepository.class); }
        @Bean StudentRepository students() { return mock(StudentRepository.class); }
        @Bean TeacherRepository teachers() { return mock(TeacherRepository.class); }
    }

    @Autowired ProfileService profiles;
    @Autowired AccountRepository accounts;
    @Autowired StudentRepository students;
    @Autowired TeacherRepository teachers;

    @BeforeEach
    void resetMocks() { reset(accounts, students, teachers); }

    private Account account(Long id, String username, Role role) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setRole(role);
        return account;
    }

    private ProfileForm form() {
        ProfileForm form = new ProfileForm();
        form.setName("Test Student");
        form.setRoll("00123");
        return form;
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void ownUpdateTargetsOnlyLoggedInStudentsRecord() {
        Account own = account(3L, "student1", Role.STUDENT);
        Student existing = new Student();
        existing.setAccount(own);
        when(accounts.findByUsername("student1")).thenReturn(Optional.of(own));
        when(students.findByAccountId(3L)).thenReturn(Optional.of(existing));
        ProfileForm form = form();

        profiles.updateOwnProfile(form);

        assertEquals("00123", existing.getRoll());
        verify(students).save(existing);
        verify(students, never()).findById(anyLong());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void studentCannotUseAdminProfileMethods() {
        assertThrows(AccessDeniedException.class, () -> profiles.getManagedProfile(2L));
        assertThrows(AccessDeniedException.class, () -> profiles.updateManagedProfile(2L, form()));
        assertThrows(AccessDeniedException.class, () -> profiles.deleteManagedAccount(2L));
        verifyNoInteractions(accounts, students, teachers);
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void teacherUpdatesOnlyOwnCourse() {
        Account own = account(2L, "teacher1", Role.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setAccount(own);
        when(accounts.findByUsername("teacher1")).thenReturn(Optional.of(own));
        when(teachers.findByAccountId(2L)).thenReturn(Optional.of(teacher));
        ProfileForm form = form();
        form.setCourse("SEPM");
        profiles.updateOwnProfile(form);

        assertEquals("SEPM", teacher.getCourse());
        verify(teachers).save(teacher);
        verifyNoInteractions(students);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void selfDeletionTargetsLoggedInAccount() {
        Account own = account(3L, "student1", Role.STUDENT);
        Student student = new Student();
        when(accounts.findByUsername("student1")).thenReturn(Optional.of(own));
        when(students.findByAccountId(3L)).thenReturn(Optional.of(student));
        when(teachers.findByAccountId(3L)).thenReturn(Optional.empty());

        profiles.deleteOwnAccount();

        verify(students).delete(student);
        verify(accounts).delete(own);
        verify(accounts, never()).findById(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreatesProfileForOnlyTheSelectedLogin() {
        Account target = account(3L, "student1", Role.STUDENT);
        when(accounts.findById(3L)).thenReturn(Optional.of(target));
        when(students.findByAccountId(3L)).thenReturn(Optional.empty());
        ProfileForm form = form();

        profiles.updateManagedProfile(3L, form);
        verify(students).save(argThat(student ->
                student.getAccount() == target && student.getRoll().equals("00123")));
        verify(students, never()).findById(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void singleAdminCannotBeDeleted() {
        when(accounts.findById(1L)).thenReturn(Optional.of(account(1L, "admin", Role.ADMIN)));
        var error = assertThrows(ResponseStatusException.class, () -> profiles.deleteManagedAccount(1L));
        assertEquals(403, error.getStatusCode().value());
        verify(accounts, never()).delete(any());
    }
}
