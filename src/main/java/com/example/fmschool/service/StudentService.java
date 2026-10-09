package com.example.fmschool.service;

import com.example.fmschool.dto.StudentForm;
import com.example.fmschool.entity.Student;
import com.example.fmschool.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
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
}


