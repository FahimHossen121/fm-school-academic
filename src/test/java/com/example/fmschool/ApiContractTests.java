package com.example.fmschool;

import com.example.fmschool.controller.*;
import com.example.fmschool.config.SecurityConfig;
import com.example.fmschool.dto.*;
import com.example.fmschool.entity.Role;
import com.example.fmschool.repository.AccountRepository;
import com.example.fmschool.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitWebConfig(ApiContractTests.Config.class)
class ApiContractTests {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, StudentController.class, TeacherApiController.class,
            SelfApiController.class, EnrollmentApiController.class, ApiExceptionHandler.class})
    static class Config {
        @Bean AccountRepository accounts() { return mock(AccountRepository.class); }
        @Bean PeopleApiService people() { return mock(PeopleApiService.class); }
        @Bean ProfileService profiles() { return mock(ProfileService.class); }
        @Bean EnrollmentService enrollments() { return mock(EnrollmentService.class); }
    }

    @Autowired WebApplicationContext context;
    @Autowired PeopleApiService people;
    @Autowired ProfileService profiles;
    @Autowired EnrollmentService enrollments;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        people = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(people);
        profiles = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(profiles);
        enrollments = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(enrollments);
        reset(people, profiles, enrollments);
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    private final String newStudent = """
        {"username":"apiStudent","password":"example123",
         "profile":{"name":"Api Student","roll":"00123"}}
        """;

    @Test
    void anonymousApiGetsJson401RatherThanHtmlRedirect() throws Exception {
        mvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void nonAdminCannotReadGeneralCollections() throws Exception {
        mvc.perform(get("/api/students").with(user("teacher2").roles("TEACHER")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/teachers").with(user("student2").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(people);
    }

    @Test
    void listReturnsDtoWithoutPasswordFields() throws Exception {
        when(people.students()).thenReturn(List.of(new PeopleApiService.StudentView(
                20L, 30L, "student2", "student2", "00123")));
        mvc.perform(get("/api/students").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].roll").value("00123"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void studentCreationReturns201AndLocation() throws Exception {
        when(people.createStudent(any())).thenReturn(new PeopleApiService.StudentView(
                20L, 30L, "apiStudent", "Api Student", "00123"));
        mvc.perform(post("/api/students").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(newStudent))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/students/20"))
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void teacherCreationReturns201() throws Exception {
        when(people.createTeacher(any())).thenReturn(new PeopleApiService.TeacherView(
                10L, 15L, "apiTeacher", "Api Teacher", "SEPM"));
        mvc.perform(post("/api/teachers").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content("""
                            {"username":"apiTeacher","password":"example123",
                             "profile":{"name":"Api Teacher","course":"SEPM"}}
                            """))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/teachers/10"));
    }

    @Test
    void invalidInputReturnsFieldErrorsWithoutSaving() throws Exception {
        mvc.perform(post("/api/students").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content("""
                            {"username":"newStudent","password":"short",
                             "profile":{"name":"Fahim123","roll":"abc"}}
                            """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.password").exists());
        verify(people, never()).createStudent(any());
    }

    @Test
    void missingStudentReturnsJson404() throws Exception {
        when(people.student(999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        mvc.perform(get("/api/students/999").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Student not found"));
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        when(people.createStudent(any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken"));
        mvc.perform(post("/api/students").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(newStudent))
                .andExpect(status().isConflict());
    }

    @Test
    void missingCsrfBlocksWritesWithJson403() throws Exception {
        mvc.perform(post("/api/students").with(user("admin").roles("ADMIN"))
                        .contentType("application/json").content(newStudent))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(people);
    }

    @Test
    void deleteReturns204WithoutResponseBody() throws Exception {
        mvc.perform(delete("/api/students/20").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(people).deleteStudent(20L);
    }

    @Test
    void selfUpdateUsesOnlyAuthenticatedProfileService() throws Exception {
        var form = new ProfileForm();
        form.setName("Student Two");
        form.setRoll("00123");
        when(profiles.getOwnProfile()).thenReturn(new ProfileService.View(30L, "student2", Role.STUDENT, 20L, form));
        mvc.perform(put("/api/me").with(user("student2").roles("STUDENT")).with(csrf())
                        .contentType("application/json").content("{\"name\":\"Student Two\",\"roll\":\"00123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("student2"));
        verify(profiles).updateOwnProfile(any());
        verifyNoInteractions(people);
    }

    @Test
    void invalidMarksAreRejectedBeforeEnrollmentServiceCall() throws Exception {
        mvc.perform(put("/api/enrollments/7/student").with(user("teacher2").roles("TEACHER")).with(csrf())
                        .contentType("application/json").content("{\"name\":\"Student Two\",\"roll\":\"00123\",\"marks\":101}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.marks").exists());
        verify(enrollments, never()).updateEnrolledStudent(anyLong(), any());
    }

    @Test
    void csrfEndpointSuppliesHeaderAndToken() throws Exception {
        mvc.perform(get("/api/csrf").with(user("teacher2").roles("TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/students").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }
}
