package com.example.fmschool;

import com.example.fmschool.dto.EnrolledStudentForm;
import com.example.fmschool.entity.Account;
import com.example.fmschool.entity.Enrollment;
import com.example.fmschool.entity.EnrollmentStatus;
import com.example.fmschool.entity.Role;
import com.example.fmschool.entity.Student;
import com.example.fmschool.entity.Teacher;
import com.example.fmschool.repository.AccountRepository;
import com.example.fmschool.repository.EnrollmentRepository;
import com.example.fmschool.repository.StudentRepository;
import com.example.fmschool.repository.TeacherRepository;
import com.example.fmschool.service.EnrollmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnrollmentServiceUnitTests {
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final TeacherRepository teachers = mock(TeacherRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final EnrollmentService service = new EnrollmentService(enrollments, accounts, teachers, students);

    private Account teacherAccount;
    private Account studentAccount;
    private Teacher teacher;
    private Student student;
    private Enrollment application;

    @BeforeEach
    void setUp() {
        teacherAccount = account(2L, "teacher1", Role.TEACHER);
        studentAccount = account(3L, "student1", Role.STUDENT);
        teacher = new Teacher();
        teacher.setId(10L);
        teacher.setAccount(teacherAccount);
        student = student(20L, "Student One", studentAccount);
        application = enrollment(7L, teacher, EnrollmentStatus.PENDING);

        when(enrollments.findStudentId(7L)).thenReturn(Optional.of(20L));
        when(students.findLockedById(20L)).thenReturn(Optional.of(student));
        when(enrollments.findLockedById(7L)).thenReturn(Optional.of(application));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void retryingAcceptanceReturnsConflictWithoutSavingAgain() {
        signInAs(teacherAccount);

        service.respond(7L, true);
        assertEquals(EnrollmentStatus.ACCEPTED, application.getStatus());
        verify(enrollments).save(application);
        clearInvocations(enrollments);

        assertStatus(HttpStatus.CONFLICT, () -> service.respond(7L, true));
        assertEquals(EnrollmentStatus.ACCEPTED, application.getStatus());
        verify(enrollments, never()).save(any());
    }

    @Test
    void rejectedApplicationReopensAsInvitationWithStudentAsRecipient() {
        signInAs(teacherAccount);
        service.respond(7L, false);
        assertEquals(EnrollmentStatus.REJECTED, application.getStatus());

        when(teachers.findByAccountId(2L)).thenReturn(Optional.of(teacher));
        when(enrollments.findLockedPair(10L, 20L)).thenReturn(Optional.of(application));
        clearInvocations(enrollments);

        service.invite(20L);

        assertEquals(EnrollmentStatus.PENDING, application.getStatus());
        assertEquals(Role.TEACHER, application.getRequestedBy());
        verify(enrollments).save(same(application));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.respond(7L, true));
        assertEquals(EnrollmentStatus.PENDING, application.getStatus());

        signInAs(studentAccount);
        service.respond(7L, true);
        assertEquals(EnrollmentStatus.ACCEPTED, application.getStatus());
    }

    @Test
    void teacherPickerExcludesOccupiedStudentsAndProfilesWithoutAccounts() {
        signInAs(teacherAccount);
        when(teachers.findByAccountId(2L)).thenReturn(Optional.of(teacher));
        Student occupied = student(21L, "Occupied", account(4L, "occupied", Role.STUDENT));
        Student legacy = student(22L, "Legacy", null);
        when(students.findAll()).thenReturn(List.of(student, occupied, legacy));
        when(enrollments.findStudentIdsByStatus(EnrollmentStatus.ACCEPTED)).thenReturn(List.of(21L));

        var board = service.getBoard();

        assertEquals(List.of(new EnrollmentService.StudentOption(20L, "Student One")), board.students());
    }

    @Test
    void enrolledStudentKeepsPendingRowsButCannotPickANewTeacher() {
        signInAs(studentAccount);
        when(students.findByAccountId(3L)).thenReturn(Optional.of(student));
        Teacher otherTeacher = new Teacher();
        otherTeacher.setId(11L);
        Enrollment accepted = enrollment(8L, otherTeacher, EnrollmentStatus.ACCEPTED);
        when(enrollments.findForAccount(3L)).thenReturn(List.of(application, accepted));
        when(teachers.findAll()).thenReturn(List.of(teacher, otherTeacher));

        var board = service.getBoard();

        assertTrue(board.teachers().isEmpty());
        assertEquals(List.of(7L, 8L), board.rows().stream().map(EnrollmentService.Row::id).toList());
        assertEquals(EnrollmentStatus.PENDING, board.rows().getFirst().status());
    }

    @Test
    void clearingAnExistingMarkRestoresTheUngradedState() {
        signInAs(teacherAccount);
        application.setStatus(EnrollmentStatus.ACCEPTED);
        application.setMarks(new BigDecimal("78.25"));
        EnrolledStudentForm form = new EnrolledStudentForm();
        form.setName(student.getName());
        form.setRoll(student.getRoll());

        service.updateEnrolledStudent(7L, form);

        assertNull(application.getMarks());
        verify(enrollments).save(application);
    }

    @Test
    void teacherWithAnAccountButNoProfileCannotSendAnInvitation() {
        signInAs(teacherAccount);
        when(teachers.findByAccountId(2L)).thenReturn(Optional.empty());

        var conflict = assertStatus(HttpStatus.CONFLICT, () -> service.invite(20L));

        assertEquals("Complete My Profile before inviting", conflict.getReason());
        verify(enrollments, never()).save(any());
    }

    private void signInAs(Account account) {
        when(accounts.findByUsername(account.getUsername())).thenReturn(Optional.of(account));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                account.getUsername(), null, "ROLE_" + account.getRole().name()));
    }

    private static ResponseStatusException assertStatus(HttpStatus status, Runnable action) {
        var error = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, error.getStatusCode());
        return error;
    }

    private Enrollment enrollment(long id, Teacher assignedTeacher, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setTeacher(assignedTeacher);
        enrollment.setStudent(student);
        enrollment.setRequestedBy(Role.STUDENT);
        enrollment.setStatus(status);
        return enrollment;
    }

    private static Account account(long id, String username, Role role) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setRole(role);
        return account;
    }

    private static Student student(long id, String name, Account account) {
        Student student = new Student();
        ReflectionTestUtils.setField(student, "id", id);
        student.setName(name);
        student.setRoll("00123");
        student.setAccount(account);
        return student;
    }
}
