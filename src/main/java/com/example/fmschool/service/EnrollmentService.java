package com.example.fmschool.service;

import com.example.fmschool.entity.*;
import com.example.fmschool.dto.EnrolledStudentForm;
import com.example.fmschool.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Objects;

@Service
@Validated
@PreAuthorize("isAuthenticated()")
public class EnrollmentService {
    private final EnrollmentRepository enrollments;
    private final AccountRepository accounts;
    private final TeacherRepository teachers;
    private final StudentRepository students;

    public EnrollmentService(EnrollmentRepository enrollments, AccountRepository accounts,
                             TeacherRepository teachers, StudentRepository students) {
        this.enrollments = enrollments;
        this.accounts = accounts;
        this.teachers = teachers;
        this.students = students;
    }

    public record TeacherOption(Long id, String name, String course) {}
    public record StudentOption(Long id, String name) {}
    public record Row(Long id, String teacher, String course, String student, String roll,
                      EnrollmentStatus status, Role requestedBy,
                      boolean canRespond, boolean canRemove, boolean canEdit,
                      java.math.BigDecimal marks) {}
    public record Board(Role role, boolean ready, List<TeacherOption> teachers,
                        List<StudentOption> students, List<Row> rows) {}

    public record EditView(Long enrollmentId, String username, String course, EnrolledStudentForm form) {}

    @Transactional(readOnly = true)
    public Board getBoard() {
        Account actor = currentAccount();
        boolean admin = actor.getRole() == Role.ADMIN;
        boolean ready = admin || (actor.getRole() == Role.TEACHER
                ? teachers.findByAccountId(actor.getId()).isPresent()
                : students.findByAccountId(actor.getId()).isPresent());
        List<Enrollment> records = admin ? enrollments.findAllWithProfiles()
                : enrollments.findForAccount(actor.getId());
        boolean alreadyEnrolled = actor.getRole() == Role.STUDENT
                && records.stream().anyMatch(e -> e.getStatus() == EnrollmentStatus.ACCEPTED);
        var enrolledStudentIds = new java.util.HashSet<>(
                enrollments.findStudentIdsByStatus(EnrollmentStatus.ACCEPTED));
        List<TeacherOption> teacherOptions = actor.getRole() == Role.TEACHER || alreadyEnrolled ? List.of()
                : teachers.findAll().stream()
                    .map(t -> new TeacherOption(t.getId(), t.getName(), t.getCourse())).toList();
        // The invitation directory exposes names, not another Student's roll/profile.
        List<StudentOption> studentOptions = actor.getRole() == Role.STUDENT ? List.of()
                : students.findAll().stream().filter(s -> s.getAccount() != null
                        && !enrolledStudentIds.contains(s.getId()))
                    .map(s -> new StudentOption(s.getId(), s.getName())).toList();
        return new Board(actor.getRole(), ready, teacherOptions, studentOptions,
                records.stream().map(e -> row(e, actor)).toList());
    }

    @Transactional
    @PreAuthorize("hasRole('STUDENT')")
    public void apply(Long teacherId) {
        Account actor = currentAccount();
        requireRole(actor, Role.STUDENT);
        Student student = students.findByAccountId(actor.getId()).orElseThrow(() ->
                conflict("Complete My Profile before applying"));
        request(teacher(teacherId), student, Role.STUDENT);
    }

    @Transactional
    @PreAuthorize("hasRole('TEACHER')")
    public void invite(Long studentId) {
        Account actor = currentAccount();
        requireRole(actor, Role.TEACHER);
        Teacher teacher = teachers.findByAccountId(actor.getId()).orElseThrow(() ->
                conflict("Complete My Profile before inviting"));
        request(teacher, student(studentId), Role.TEACHER);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void associate(Long teacherId, Long studentId) {
        requireRole(currentAccount(), Role.ADMIN);
        Teacher teacher = teacher(teacherId);
        Student student = student(studentId);
        requireAvailable(student.getId(), teacherId);
        Enrollment enrollment = enrollments.findLockedPair(teacherId, studentId).orElseGet(Enrollment::new);
        enrollment.setTeacher(teacher);
        enrollment.setStudent(student);
        enrollment.setStatus(EnrollmentStatus.ACCEPTED);
        enrollment.setRequestedBy(Role.ADMIN);
        enrollments.save(enrollment);
    }

    @Transactional
    public void respond(Long id, boolean accept) {
        Account actor = currentAccount();
        Enrollment enrollment = enrollment(id);
        // Check who may act before exposing the current request state.
        if (actor.getRole() != Role.ADMIN && !isRecipient(enrollment, actor)) {
            throw forbidden();
        }
        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw conflict("Only pending requests can be accepted or rejected");
        }
        if (accept) {
            requireAvailable(enrollment.getStudent().getId(), enrollment.getTeacher().getId());
        }
        enrollment.setStatus(accept ? EnrollmentStatus.ACCEPTED : EnrollmentStatus.REJECTED);
        enrollments.save(enrollment);
    }

    @Transactional
    public void remove(Long id) {
        Account actor = currentAccount();
        Enrollment enrollment = enrollment(id);
        if (actor.getRole() != Role.ADMIN && !isParticipant(enrollment, actor)) {
            throw forbidden();
        }
        if (actor.getRole() != Role.ADMIN && enrollment.getStatus() != EnrollmentStatus.ACCEPTED) {
            throw conflict("Only accepted associations can be removed");
        }
        enrollments.delete(enrollment);
    }

    private void request(Teacher teacher, Student student, Role sender) {
        student = lockStudent(student.getId());
        requireAvailable(student.getId(), teacher.getId());
        Enrollment enrollment = enrollments.findLockedPair(teacher.getId(), student.getId())
                .orElseGet(Enrollment::new);
        if (enrollment.getStatus() == EnrollmentStatus.ACCEPTED) {
            throw conflict("This Student is already associated with this Teacher");
        }
        if (enrollment.getStatus() == EnrollmentStatus.PENDING) {
            throw conflict("A request is already pending; its recipient must respond");
        }
        // A rejected pair can apply/invite again, without duplicating the association.
        enrollment.setTeacher(teacher);
        enrollment.setStudent(student);
        enrollment.setRequestedBy(sender);
        enrollment.setStatus(EnrollmentStatus.PENDING);
        enrollments.save(enrollment);
    }

    private Enrollment enrollment(Long id) {
        Long studentId = enrollments.findStudentId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        // Consistent order: lock the Student before the enrollment.
        // This serializes competing acceptances for different Teachers.
        lockStudent(studentId);
        return enrollments.findLockedById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
    }

    private Teacher teacher(Long id) {
        return teachers.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
    }

    private Student student(Long id) {
        Student student = lockStudent(id);
        if (student.getAccount() == null) {
            throw conflict("This Student needs a login and completed personal profile first");
        }
        return student;
    }

    private Student lockStudent(Long id) {
        return students.findLockedById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    private void requireAvailable(Long studentId, Long teacherId) {
        if (enrollments.existsByStudentIdAndStatusAndTeacherIdNot(
                studentId, EnrollmentStatus.ACCEPTED, teacherId)) {
            throw conflict("This Student already belongs to another course. Remove that association first.");
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public EditView getStudentForEdit(Long id) {
        Enrollment enrollment = enrollments.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        requireEditor(enrollment, currentAccount());
        EnrolledStudentForm form = new EnrolledStudentForm();
        form.setName(enrollment.getStudent().getName());
        form.setRoll(enrollment.getStudent().getRoll());
        form.setMarks(enrollment.getMarks());
        return new EditView(id, enrollment.getStudent().getAccount().getUsername(),
                enrollment.getTeacher().getCourse(), form);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void updateEnrolledStudent(Long id, @Valid EnrolledStudentForm form) {
        Enrollment enrollment = enrollment(id);
        requireEditor(enrollment, currentAccount());
        Student student = enrollment.getStudent();
        student.setName(form.getName());
        student.setRoll(form.getRoll());
        enrollment.setMarks(form.getMarks());
        students.save(student);
        enrollments.save(enrollment);
        // Account username/password/role are deliberately untouched.
    }

    private void requireEditor(Enrollment enrollment, Account actor) {
        if (enrollment.getStatus() != EnrollmentStatus.ACCEPTED
                || (actor.getRole() != Role.ADMIN
                    && !(actor.getRole() == Role.TEACHER
                        && Objects.equals(enrollment.getTeacher().getAccount().getId(), actor.getId())))) {
            throw forbidden();
        }
    }

    private Account currentAccount() {
        return accounts.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private boolean isParticipant(Enrollment e, Account actor) {
        return (e.getTeacher().getAccount() != null
                    && Objects.equals(e.getTeacher().getAccount().getId(), actor.getId()))
                || (e.getStudent().getAccount() != null
                    && Objects.equals(e.getStudent().getAccount().getId(), actor.getId()));
    }

    private boolean isRecipient(Enrollment e, Account actor) {
        return (e.getRequestedBy() == Role.STUDENT && actor.getRole() == Role.TEACHER
                    && Objects.equals(e.getTeacher().getAccount().getId(), actor.getId()))
                || (e.getRequestedBy() == Role.TEACHER && actor.getRole() == Role.STUDENT
                    && e.getStudent().getAccount() != null
                    && Objects.equals(e.getStudent().getAccount().getId(), actor.getId()));
    }

    private Row row(Enrollment e, Account actor) {
        boolean admin = actor.getRole() == Role.ADMIN;
        boolean accepted = e.getStatus() == EnrollmentStatus.ACCEPTED;
        return new Row(e.getId(), e.getTeacher().getName(), e.getTeacher().getCourse(),
                e.getStudent().getName(), accepted || admin ? e.getStudent().getRoll() : null,
                e.getStatus(), e.getRequestedBy(),
                e.getStatus() == EnrollmentStatus.PENDING && (admin || isRecipient(e, actor)),
                admin || (accepted && isParticipant(e, actor)),
                accepted && (admin || (actor.getRole() == Role.TEACHER
                    && Objects.equals(e.getTeacher().getAccount().getId(), actor.getId()))),
                accepted || admin ? e.getMarks() : null);
    }

    private void requireRole(Account actor, Role role) {
        if (actor.getRole() != role) {
            throw forbidden();
        }
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
