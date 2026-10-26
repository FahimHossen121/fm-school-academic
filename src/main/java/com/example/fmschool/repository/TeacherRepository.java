package com.example.fmschool.repository;

import com.example.fmschool.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByAccountId(Long accountId);
}
