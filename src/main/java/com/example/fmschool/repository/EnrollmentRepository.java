package com.example.fmschool.repository;

import com.example.fmschool.entity.Enrollment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndStatusAndTeacherIdNot(
            Long studentId, com.example.fmschool.entity.EnrollmentStatus status, Long teacherId);

    @Query("select e.student.id from Enrollment e where e.id = :id")
    Optional<Long> findStudentId(@Param("id") Long id);

    @Query("select e.student.id from Enrollment e where e.status = :status")
    List<Long> findStudentIdsByStatus(
            @Param("status") com.example.fmschool.entity.EnrollmentStatus status);

    @Query("select e from Enrollment e join fetch e.teacher join fetch e.student order by e.id desc")
    List<Enrollment> findAllWithProfiles();

    @Query("select e from Enrollment e join fetch e.teacher t join fetch e.student s "
            + "where t.account.id = :accountId or s.account.id = :accountId order by e.id desc")
    List<Enrollment> findForAccount(@Param("accountId") Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.id = :id")
    Optional<Enrollment> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.teacher.id = :teacherId and e.student.id = :studentId")
    Optional<Enrollment> findLockedPair(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId);
}
