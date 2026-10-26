package com.example.fmschool;

import com.example.fmschool.entity.*;
import com.example.fmschool.dto.EnrolledStudentForm;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import com.example.fmschool.repository.*;
import com.example.fmschool.service.EnrollmentService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(EnrollmentWorkflowTests.Config.class)
class EnrollmentWorkflowTests {
    @Configuration
    @EnableMethodSecurity
    @Import(EnrollmentService.class)
    static class Config {
        @Bean
        static org.springframework.validation.beanvalidation.MethodValidationPostProcessor validation() {
            return new org.springframework.validation.beanvalidation.MethodValidationPostProcessor();
        }
        @Bean EnrollmentRepository enrollments() { return mock(EnrollmentRepository.class); }
        @Bean AccountRepository accounts() { return mock(AccountRepository.class); }
        @Bean TeacherRepository teachers() { return mock(TeacherRepository.class); }
        @Bean StudentRepository students() { return mock(StudentRepository.class); }
    }

    @Autowired EnrollmentService service;
    @Autowired EnrollmentRepository enrollments;
    @Autowired AccountRepository accounts;
    @Autowired TeacherRepository teachers;
    @Autowired StudentRepository students;
    Account studentAccount, teacherAccount, adminAccount;
    Teacher teacher;
    Student student;
    Enrollment pending;

    @BeforeEach
    void setup() {
        reset(enrollments, accounts, teachers, students);
        teacherAccount = account(2L, "teacher1", Role.TEACHER);
        studentAccount = account(3L, "student1", Role.STUDENT);
        adminAccount = account(1L, "admin", Role.ADMIN);
        when(accounts.findByUsername("teacher1")).thenReturn(Optional.of(teacherAccount));
        when(accounts.findByUsername("student1")).thenReturn(Optional.of(studentAccount));
        when(accounts.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        teacher = new Teacher();
        teacher.setId(10L);
        teacher.setAccount(teacherAccount);
        teacher.setName("Teacher One");
        teacher.setCourse("SEPM");
        student = new Student();
        ReflectionTestUtils.setField(student, "id", 20L);
        student.setAccount(studentAccount);
        student.setName("Student One");
        student.setRoll("00123");
        when(teachers.findById(10L)).thenReturn(Optional.of(teacher));
        when(teachers.findByAccountId(2L)).thenReturn(Optional.of(teacher));
        when(students.findById(20L)).thenReturn(Optional.of(student));
        when(students.findLockedById(20L)).thenReturn(Optional.of(student));
        when(students.findByAccountId(3L)).thenReturn(Optional.of(student));
        when(enrollments.findLockedPair(anyLong(), anyLong())).thenReturn(Optional.empty());
        pending = new Enrollment();
        pending.setId(7L);
        pending.setTeacher(teacher);
        pending.setStudent(student);
        pending.setStatus(EnrollmentStatus.PENDING);
        pending.setRequestedBy(Role.STUDENT);
        when(enrollments.findLockedById(7L)).thenReturn(Optional.of(pending));
        when(enrollments.findById(7L)).thenReturn(Optional.of(pending));
        when(enrollments.findStudentId(7L)).thenReturn(Optional.of(20L));
    }

    private Account account(Long id, String name, Role role) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(name);
        account.setRole(role);
        return account;
    }

    private void assertStatus(int status, Runnable operation) {
        var error = assertThrows(ResponseStatusException.class, operation::run);
        assertEquals(status, error.getStatusCode().value());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void applicationUsesOwnStudentAndStartsPending() {
        service.apply(10L);
        verify(enrollments).save(argThat(e -> e.getStudent() == student && e.getTeacher() == teacher
                && e.getStatus() == EnrollmentStatus.PENDING && e.getRequestedBy() == Role.STUDENT));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void invitationUsesOwnTeacherAndStartsPending() {
        service.invite(20L);
        verify(enrollments).save(argThat(e -> e.getTeacher() == teacher
                && e.getRequestedBy() == Role.TEACHER && e.getStatus() == EnrollmentStatus.PENDING));
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void senderCannotAcceptOwnApplication() {
        assertStatus(403, () -> service.respond(7L, true));
        verify(enrollments, never()).save(any());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void recipientTeacherCanAcceptApplication() {
        service.respond(7L, true);
        assertEquals(EnrollmentStatus.ACCEPTED, pending.getStatus());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void recipientStudentCanRejectInvitation() {
        pending.setRequestedBy(Role.TEACHER);
        service.respond(7L, false);
        assertEquals(EnrollmentStatus.REJECTED, pending.getStatus());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void recipientStudentCanAcceptInvitation() {
        pending.setRequestedBy(Role.TEACHER);
        service.respond(7L, true);
        assertEquals(EnrollmentStatus.ACCEPTED, pending.getStatus());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void unrelatedTeacherCannotRespondOrRemove() {
        Account other = account(5L, "teacher1", Role.TEACHER);
        when(accounts.findByUsername("teacher1")).thenReturn(Optional.of(other));
        assertStatus(403, () -> service.respond(7L, true));
        assertStatus(403, () -> service.remove(7L));
        verify(enrollments, never()).delete(any());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void pendingSenderCannotRemoveAndDuplicateDoesNotAutoAccept() {
        when(enrollments.findLockedPair(10L, 20L)).thenReturn(Optional.of(pending));
        assertStatus(409, () -> service.remove(7L));
        assertStatus(409, () -> service.apply(10L));
        assertEquals(EnrollmentStatus.PENDING, pending.getStatus());
        verify(enrollments, never()).save(any());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void oppositeDirectionRequestCannotOverwritePendingApplication() {
        when(enrollments.findLockedPair(10L, 20L)).thenReturn(Optional.of(pending));
        assertStatus(409, () -> service.invite(20L));
        assertEquals(Role.STUDENT, pending.getRequestedBy());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void participantRemovesOnlyAssociationNotPeople() {
        pending.setStatus(EnrollmentStatus.ACCEPTED);
        service.remove(7L);
        verify(enrollments).delete(pending);
        verify(accounts, never()).delete(any());
        verify(teachers, never()).delete(any());
        verify(students, never()).delete(any());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void rejectedPairCanApplyAgainWithoutSecondRow() {
        pending.setStatus(EnrollmentStatus.REJECTED);
        when(enrollments.findLockedPair(10L, 20L)).thenReturn(Optional.of(pending));
        service.apply(10L);
        verify(enrollments).save(pending);
        assertEquals(7L, pending.getId());
        assertEquals(EnrollmentStatus.PENDING, pending.getStatus());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAssociatesDirectlyAndCanRemovePendingRequests() {
        service.associate(10L, 20L);
        verify(enrollments).save(argThat(e -> e.getStatus() == EnrollmentStatus.ACCEPTED
                && e.getRequestedBy() == Role.ADMIN));
        service.remove(7L);
        verify(enrollments).delete(pending);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void studentCannotInvokeTeacherOrAdminOperations() {
        assertThrows(AccessDeniedException.class, () -> service.invite(20L));
        assertThrows(AccessDeniedException.class, () -> service.associate(10L, 20L));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void boardFiltersByOwnAccountAndHidesRollUntilAccepted() {
        when(enrollments.findForAccount(2L)).thenReturn(List.of(pending));
        when(students.findAll()).thenReturn(List.of(student));
        var board = service.getBoard();
        assertNull(board.rows().getFirst().roll());
        assertTrue(board.rows().getFirst().canRespond());
        verify(enrollments, never()).findAllWithProfiles();
        pending.setStatus(EnrollmentStatus.ACCEPTED);
        assertEquals("00123", service.getBoard().rows().getFirst().roll());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void studentCanSendMultiplePendingApplicationsBeforeAcceptance() {
        Teacher another = new Teacher();
        another.setId(11L);
        another.setAccount(account(4L, "teacher2", Role.TEACHER));
        when(teachers.findById(11L)).thenReturn(Optional.of(another));
        service.apply(10L);
        service.apply(11L);
        verify(enrollments, times(2)).save(any());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void teacherCanInviteMoreThanOneStudent() {
        Student another = new Student();
        ReflectionTestUtils.setField(another, "id", 21L);
        another.setAccount(account(4L, "student2", Role.STUDENT));
        when(students.findById(21L)).thenReturn(Optional.of(another));
        when(students.findLockedById(21L)).thenReturn(Optional.of(another));
        service.invite(20L);
        service.invite(21L);
        verify(enrollments, times(2)).save(any());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void studentAlreadyInAnotherCourseCannotApplyOrAccept() {
        when(enrollments.existsByStudentIdAndStatusAndTeacherIdNot(20L, EnrollmentStatus.ACCEPTED, 10L))
                .thenReturn(true);
        assertStatus(409, () -> service.apply(10L));
        pending.setRequestedBy(Role.TEACHER);
        assertStatus(409, () -> service.respond(7L, true));
        assertEquals(EnrollmentStatus.PENDING, pending.getStatus());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void teacherCannotInviteOrAcceptStudentFromAnotherCourse() {
        when(enrollments.existsByStudentIdAndStatusAndTeacherIdNot(20L, EnrollmentStatus.ACCEPTED, 10L))
                .thenReturn(true);
        assertStatus(409, () -> service.invite(20L));
        assertStatus(409, () -> service.respond(7L, true));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAlsoRespectsSingleCourseRule() {
        when(enrollments.existsByStudentIdAndStatusAndTeacherIdNot(20L, EnrollmentStatus.ACCEPTED, 10L))
                .thenReturn(true);
        assertStatus(409, () -> service.associate(10L, 20L));
    }

    private EnrolledStudentForm editForm() {
        EnrolledStudentForm form = new EnrolledStudentForm();
        form.setName("Updated Student");
        form.setRoll("00999");
        form.setMarks(new BigDecimal("85.50"));
        return form;
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void associatedTeacherEditsProfileAndMarksButNotLogin() {
        pending.setStatus(EnrollmentStatus.ACCEPTED);
        studentAccount.setPasswordHash("unchanged-hash");
        service.updateEnrolledStudent(7L, editForm());
        assertEquals("Updated Student", student.getName());
        assertEquals("00999", student.getRoll());
        assertEquals(new BigDecimal("85.50"), pending.getMarks());
        assertEquals("student1", studentAccount.getUsername());
        assertEquals("unchanged-hash", studentAccount.getPasswordHash());
        assertEquals(Role.STUDENT, studentAccount.getRole());
        verify(accounts, never()).save(any());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void pendingAssociationDoesNotPermitEditing() {
        assertStatus(403, () -> service.getStudentForEdit(7L));
        assertStatus(403, () -> service.updateEnrolledStudent(7L, editForm()));
        verify(students, never()).save(any());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void differentTeacherCannotOpenOrSubmitEdit() {
        pending.setStatus(EnrollmentStatus.ACCEPTED);
        when(accounts.findByUsername("teacher1"))
                .thenReturn(Optional.of(account(99L, "teacher1", Role.TEACHER)));
        assertStatus(403, () -> service.getStudentForEdit(7L));
        assertStatus(403, () -> service.updateEnrolledStudent(7L, editForm()));
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void studentCannotEditOwnMarksEvenThroughDirectServiceCall() {
        pending.setStatus(EnrollmentStatus.ACCEPTED);
        assertThrows(AccessDeniedException.class, () -> service.getStudentForEdit(7L));
        assertThrows(AccessDeniedException.class, () -> service.updateEnrolledStudent(7L, editForm()));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void marksMustBeWithinRangeAndNoMoreThanTwoDecimalPlaces() {
        pending.setStatus(EnrollmentStatus.ACCEPTED);
        EnrolledStudentForm form = editForm();
        form.setMarks(new BigDecimal("101"));
        assertThrows(ConstraintViolationException.class, () -> service.updateEnrolledStudent(7L, form));
        form.setMarks(new BigDecimal("-1"));
        assertThrows(ConstraintViolationException.class, () -> service.updateEnrolledStudent(7L, form));
        form.setMarks(new BigDecimal("85.123"));
        assertThrows(ConstraintViolationException.class, () -> service.updateEnrolledStudent(7L, form));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void seededDemoStudentLabelRemainsEditableButArbitraryDigitsAreRejected() {
        pending.setStatus(EnrollmentStatus.ACCEPTED);
        EnrolledStudentForm form = editForm();
        form.setName("student2");
        service.updateEnrolledStudent(7L, form);
        assertEquals("student2", student.getName());
        form.setName("Fahim123");
        assertThrows(ConstraintViolationException.class, () -> service.updateEnrolledStudent(7L, form));
    }
}
