package com.example.fmschool;

import com.example.fmschool.config.SecurityConfig;
import com.example.fmschool.controller.SignupController;
import com.example.fmschool.entity.*;
import com.example.fmschool.repository.AccountRepository;
import com.example.fmschool.service.SignupService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitWebConfig(SignupWebTests.Config.class)
class SignupWebTests {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, SignupController.class})
    static class Config {
        @Bean AccountRepository accounts() { return mock(AccountRepository.class); }
        @Bean SignupService signup() { return mock(SignupService.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired AccountRepository accounts;
    @Autowired SignupService signup;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        signup = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(signup);
        reset(accounts, signup);
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void bothSignupPagesArePublic() throws Exception {
        mvc.perform(get("/signup/student")).andExpect(status().isOk())
                .andExpect(view().name("signup")).andExpect(model().attribute("isStudent", true));
        mvc.perform(get("/signup/teacher")).andExpect(status().isOk())
                .andExpect(model().attribute("isStudent", false));
    }

    @Test
    void studentSignupIgnoresForgedAdminRole() throws Exception {
        mvc.perform(post("/signup/student").with(csrf())
                        .param("username", "newStudent").param("password", "example123")
                        .param("name", "New Student").param("roll", "0012345")
                        .param("role", "ADMIN").param("accountId", "1"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered"));
        verify(signup).registerStudent(argThat(form -> form.getUsername().equals("newStudent")));
        verify(signup, never()).registerTeacher(any());
    }

    @Test
    void teacherSignupUsesTeacherEndpoint() throws Exception {
        mvc.perform(post("/signup/teacher").with(csrf())
                        .param("username", "newTeacher").param("password", "example123")
                        .param("name", "New Teacher").param("course", "SEPM"))
                .andExpect(redirectedUrl("/login?registered"));
        verify(signup).registerTeacher(any());
    }

    @Test
    void invalidSignupReturnsFormAndClearsPassword() throws Exception {
        mvc.perform(post("/signup/student").with(csrf())
                        .param("username", "newStudent").param("password", "short")
                        .param("name", "New Student").param("roll", "abc"))
                .andExpect(status().isOk()).andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "password", "roll"))
                .andExpect(model().attribute("signupForm",
                        org.hamcrest.Matchers.hasProperty("password", org.hamcrest.Matchers.nullValue())));
        verifyNoInteractions(signup);
    }

    @Test
    void duplicateUsernameShowsReadableError() throws Exception {
        doThrow(new IllegalArgumentException("Username is already taken")).when(signup).registerStudent(any());
        mvc.perform(post("/signup/student").with(csrf())
                        .param("username", "newStudent").param("password", "example123")
                        .param("name", "New Student").param("roll", "0012345"))
                .andExpect(status().isOk()).andExpect(view().name("signup"))
                .andExpect(model().attributeHasErrors("signupForm"));
    }

    @Test
    void signupStillRequiresCsrf() throws Exception {
        mvc.perform(post("/signup/student").param("username", "newStudent"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(signup);
    }

    private void account(String username, Role role) {
        Account account = new Account();
        account.setUsername(username);
        account.setRole(role);
        account.setPasswordHash(encoder.encode("example123"));
        when(accounts.findByUsername(username)).thenReturn(Optional.of(account));
    }

    @Test
    void studentLoginRedirectsToProfile() throws Exception {
        account("student2", Role.STUDENT);
        mvc.perform(formLogin().user("student2").password("example123"))
                .andExpect(redirectedUrl("/me"));
    }

    @Test
    void teacherLoginRedirectsToProfile() throws Exception {
        account("teacher2", Role.TEACHER);
        mvc.perform(formLogin().user("teacher2").password("example123"))
                .andExpect(redirectedUrl("/me"));
    }

    @Test
    void adminLoginRedirectsToManagementHome() throws Exception {
        account("admin", Role.ADMIN);
        mvc.perform(formLogin().user("admin").password("example123"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void noPublicAdminSignupRoute() throws Exception {
        mvc.perform(get("/signup/admin").accept("text/html")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(signup);
    }
}
