package com.example.fmschool.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "enrollments", uniqueConstraints =
        @UniqueConstraint(name = "enrollments_pair_unique", columnNames = {"teacher_id", "student_id"}))
@Getter
@Setter
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by", nullable = false, length = 20)
    private Role requestedBy;

    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal marks;
}
