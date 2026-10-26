package com.example.fmschool.service;

import com.example.fmschool.dto.StudentForm;
import com.example.fmschool.entity.Student;
import com.example.fmschool.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public void createStudent(StudentForm form) {
        Student student = new Student();
        student.setName(form.getName());
        student.setRoll(form.getRoll());

        studentRepository.save(student);
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow( () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"
                ));
    }

    @Transactional
    public void updateStudent(Long id, StudentForm form) {
        Student student = getStudentById(id);
        student.setName(form.getName());
        student.setRoll(form.getRoll());
        studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        studentRepository.delete(student);
    }
}


