package com.example.fmschool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@ActiveProfiles("integration")
class EnrollmentPersistenceIT {
    private static final String TEST_PASSWORD = "TestPass123!";
    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate db;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void studentCanTransferOnlyAfterLeavingTheAcceptedCourse() throws Exception {
        var student = createPerson("students", "Student");
        var first = createPerson("teachers", "First Teacher");
        var second = createPerson("teachers", "Second Teacher");
        long firstRequest = apply(student, first);
        long secondRequest = apply(student, second);

        accept(first, firstRequest, 200);
        accept(second, secondRequest, 409);

        assertEquals("ACCEPTED", enrollmentStatus(firstRequest));
        assertEquals("PENDING", enrollmentStatus(secondRequest));

        mvc.perform(delete("/api/enrollments/{id}", firstRequest)
                        .with(user(student.username()).roles("STUDENT")).with(csrf()))
                .andExpect(status().isNoContent());
        accept(second, secondRequest, 200);

        assertEquals(0, count("select count(*) from enrollments where id = ?", firstRequest));
        assertEquals("ACCEPTED", enrollmentStatus(secondRequest));
        assertEquals(1, count("select count(*) from enrollments where student_id = ? and status = 'ACCEPTED'",
                student.profileId()));
    }

    @Test
    void teacherCreationRollsBackTheAccountWhenCourseIsMissing() throws Exception {
        String username = testUsername();
        String body = """
                {"username":"%s","password":"%s","profile":{"name":"Test Teacher"}}
                """.formatted(username, TEST_PASSWORD);

        mvc.perform(post("/api/teachers").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Course is required"));

        assertEquals(0, count("select count(*) from accounts where username = ?", username));
    }

    @Test
    void deletingATeacherRemovesTheEnrollmentButStudentCanStillSignIn() throws Exception {
        var student = createPerson("students", "Student");
        var teacher = createPerson("teachers", "Teacher");
        long request = apply(student, teacher);
        accept(teacher, request, 200);
        assertEquals("ACCEPTED", enrollmentStatus(request));

        mvc.perform(delete("/api/teachers/{id}", teacher.profileId())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());

        assertEquals(0, count("select count(*) from accounts where username = ?", teacher.username()));
        assertEquals(0, count("select count(*) from teachers where id = ?", teacher.profileId()));
        assertEquals(0, count("select count(*) from enrollments where id = ?", request));
        assertEquals(1, count("select count(*) from students where id = ?", student.profileId()));
        mvc.perform(formLogin("/login").user(student.username()).password(TEST_PASSWORD))
                .andExpect(authenticated().withUsername(student.username()))
                .andExpect(redirectedUrl("/me"));
    }

    private Person createPerson(String collection, String name) throws Exception {
        String username = testUsername();
        String body = """
                {"username":"%s","password":"%s","profile":{"name":"%s","roll":"00123","course":"SEPM"}}
                """.formatted(username, TEST_PASSWORD, name);
        mvc.perform(post("/api/" + collection).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated());
        String query = collection.equals("students")
                ? "select s.id from students s join accounts a on s.account_id = a.id where a.username = ?"
                : "select t.id from teachers t join accounts a on t.account_id = a.id where a.username = ?";
        return new Person(db.queryForObject(query, Long.class, username), username);
    }

    private long apply(Person student, Person teacher) throws Exception {
        mvc.perform(post("/api/enrollments/applications")
                        .with(user(student.username()).roles("STUDENT")).with(csrf())
                        .contentType("application/json")
                        .content("{\"teacherId\":" + teacher.profileId() + "}"))
                .andExpect(status().isCreated());
        return db.queryForObject("select id from enrollments where student_id = ? and teacher_id = ?",
                Long.class, student.profileId(), teacher.profileId());
    }

    private void accept(Person teacher, long id, int expectedStatus) throws Exception {
        mvc.perform(post("/api/enrollments/{id}/accept", id)
                        .with(user(teacher.username()).roles("TEACHER")).with(csrf()))
                .andExpect(status().is(expectedStatus));
    }

    private String enrollmentStatus(long id) {
        return db.queryForObject("select status from enrollments where id = ?", String.class, id);
    }

    private int count(String sql, Object argument) {
        return db.queryForObject(sql, Integer.class, argument);
    }

    private static String testUsername() {
        return "it_" + UUID.randomUUID().toString().replace("-", "");
    }

    private record Person(long profileId, String username) {}
}
