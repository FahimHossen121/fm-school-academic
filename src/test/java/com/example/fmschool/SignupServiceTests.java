package com.example.fmschool;

import com.example.fmschool.dto.SignupForm;
import com.example.fmschool.entity.*;
import com.example.fmschool.repository.*;
import com.example.fmschool.service.SignupService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(SignupServiceTests.Config.class)
@WithAnonymousUser
class SignupServiceTests {
    @Configuration
    @EnableMethodSecurity
    @Import(SignupService.class)
    static class Config {
        @Bean AccountRepository accounts() { return mock(AccountRepository.class); }
        @Bean StudentRepository students() { return mock(StudentRepository.class); }
        @Bean TeacherRepository teachers() { return mock(TeacherRepository.class); }
        @Bean PasswordEncoder encoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
        @Bean static org.springframework.validation.beanvalidation.MethodValidationPostProcessor validation() {
            return new org.springframework.validation.beanvalidation.MethodValidationPostProcessor();
        }
    }
    @Autowired SignupService signup;
    @Autowired AccountRepository accounts;
    @Autowired StudentRepository students;
    @Autowired TeacherRepository teachers;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setup() {
        reset(accounts, students, teachers);
        when(accounts.save(any())).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(101L);
            return account;
        });
    }

    private SignupForm form() {
        SignupForm form = new SignupForm();
        form.setUsername("newStudent");
        form.setPassword("example123");
        form.setName("New Student");
        form.setRoll("0012345");
        form.setCourse("SEPM");
        return form;
    }

    @Test
    void studentSignupHashesPasswordAndLinksProfile() {
        SignupForm form = form();
        signup.registerStudent(form);
        var capture = org.mockito.ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(capture.capture());
        Account account = capture.getValue();
        assertEquals(Role.STUDENT, account.getRole());
        assertNotEquals(form.getPassword(), account.getPasswordHash());
        assertTrue(encoder.matches(form.getPassword(), account.getPasswordHash()));
        verify(students).save(argThat(student -> student.getAccount() == account
                && student.getRoll().equals("0012345")));
        verifyNoInteractions(teachers);
    }

    @Test
    void teacherSignupCreatesOneCourseAndTeacherRole() {
        signup.registerTeacher(form());
        verify(accounts).save(argThat(account -> account.getRole() == Role.TEACHER));
        verify(teachers).save(argThat(teacher -> teacher.getCourse().equals("SEPM")
                && teacher.getAccount().getId().equals(101L)));
        verifyNoInteractions(students);
    }

    @Test
    void duplicateUsernameDoesNotCreateAnyProfile() {
        when(accounts.existsByUsername("newStudent")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> signup.registerStudent(form()));
        verify(accounts, never()).save(any());
        verifyNoInteractions(students, teachers);
    }

    @Test
    void requiredRoleDetailsCheckedBeforeAccountIsSaved() {
        SignupForm form = form();
        form.setRoll("");
        assertThrows(IllegalArgumentException.class, () -> signup.registerStudent(form));
        form.setCourse("");
        assertThrows(IllegalArgumentException.class, () -> signup.registerTeacher(form));
        verify(accounts, never()).save(any());
    }

    @Test
    void unicodePasswordCannotExceedBcryptByteLimit() {
        SignupForm form = form();
        form.setPassword("界".repeat(30));
        assertThrows(IllegalArgumentException.class, () -> signup.registerStudent(form));
        verify(accounts, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void signedInUserCannotUsePublicSignupService() {
        assertThrows(AccessDeniedException.class, () -> signup.registerStudent(form()));
        verifyNoInteractions(accounts, students, teachers);
    }
}
