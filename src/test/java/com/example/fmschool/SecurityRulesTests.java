package com.example.fmschool;

import com.example.fmschool.config.SecurityConfig;
import com.example.fmschool.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitWebConfig(SecurityRulesTests.TestConfig.class)
class SecurityRulesTests {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, Routes.class})
    static class TestConfig {
        @Bean
        AccountRepository accounts() {
            return mock(AccountRepository.class);
        }
    }

    // Isolate the real security filter chain from database and template rendering.
    @RestController
    static class Routes {
        @GetMapping({"/", "/students", "/students/1/edit", "/api/students", "/admin/accounts"})
        String page() { return "OK"; }

        @PostMapping("/students/1/delete")
        String delete() { return "deleted"; }
    }

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anonymousBrowserMustLogin() throws Exception {
        mvc.perform(get("/students").accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminCanManageStudentsAndAccounts() throws Exception {
        mvc.perform(get("/students").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/accounts").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotOpenAnotherRecordsEditUrl() throws Exception {
        mvc.perform(get("/students/1/edit").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotReadAllStudentsThroughApi() throws Exception {
        mvc.perform(get("/api/students").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotCreateAccounts() throws Exception {
        mvc.perform(post("/admin/accounts").with(user("student").roles("STUDENT")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRequiresAdminEvenWithValidCsrf() throws Exception {
        mvc.perform(post("/students/1/delete").with(user("teacher").roles("TEACHER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/students/1/delete").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void adminPostWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/students/1/delete").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void loggedInStudentCanOpenHomeAndLogout() throws Exception {
        mvc.perform(get("/").with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());
        mvc.perform(post("/logout").accept("text/html")
                        .with(user("student").roles("STUDENT")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}
