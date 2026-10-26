package com.example.fmschool.repository;

import com.example.fmschool.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    java.util.Optional<Student> findByAccountId(Long accountId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Student s where s.id = :id")
    java.util.Optional<Student> findLockedById(
            @org.springframework.data.repository.query.Param("id") Long id);
}
