package com.example.fmschool.controller;

import com.example.fmschool.dto.EnrolledStudentForm;
import com.example.fmschool.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentApiController {
    private final EnrollmentService enrollments;
    public EnrollmentApiController(EnrollmentService enrollments) { this.enrollments = enrollments; }

    public record TeacherTarget(@NotNull @Positive Long teacherId) {}
    public record StudentTarget(@NotNull @Positive Long studentId) {}
    public record Association(@NotNull @Positive Long teacherId, @NotNull @Positive Long studentId) {}

    @GetMapping
    public EnrollmentService.Board list() { return enrollments.getBoard(); }

    @PostMapping("/applications")
    public ResponseEntity<Map<String, String>> apply(@Valid @RequestBody TeacherTarget target) {
        enrollments.apply(target.teacherId());
        return created("Application sent");
    }

    @PostMapping("/invitations")
    public ResponseEntity<Map<String, String>> invite(@Valid @RequestBody StudentTarget target) {
        enrollments.invite(target.studentId());
        return created("Invitation sent");
    }

    @PostMapping("/associations")
    public ResponseEntity<Map<String, String>> associate(@Valid @RequestBody Association target) {
        enrollments.associate(target.teacherId(), target.studentId());
        return created("Association created");
    }

    @PostMapping("/{id}/accept")
    public Map<String, String> accept(@PathVariable("id") Long id) {
        enrollments.respond(id, true);
        return Map.of("message", "Request accepted");
    }

    @PostMapping("/{id}/reject")
    public Map<String, String> reject(@PathVariable("id") Long id) {
        enrollments.respond(id, false);
        return Map.of("message", "Request rejected");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable("id") Long id) {
        enrollments.remove(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/student")
    public EnrollmentService.EditView getStudent(@PathVariable("id") Long id) {
        return enrollments.getStudentForEdit(id);
    }

    @PutMapping("/{id}/student")
    public Map<String, String> editStudent(@PathVariable("id") Long id,
                                          @Valid @RequestBody EnrolledStudentForm form) {
        enrollments.updateEnrolledStudent(id, form);
        return Map.of("message", "Student information and marks saved; login unchanged");
    }

    private ResponseEntity<Map<String, String>> created(String message) {
        return ResponseEntity.created(URI.create("/api/enrollments")).body(Map.of("message", message));
    }
}
