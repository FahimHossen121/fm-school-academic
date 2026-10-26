package com.example.fmschool.controller;


import com.example.fmschool.dto.*;
import com.example.fmschool.service.PeopleApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final PeopleApiService people;

    public StudentController(PeopleApiService people){
        this.people = people;
    }

    @GetMapping
    public List<PeopleApiService.StudentView> list() {
        return people.students();
    }

    @GetMapping("/{id}")
    public PeopleApiService.StudentView get(@PathVariable("id") Long id) { return people.student(id); }

    @PostMapping
    public ResponseEntity<PeopleApiService.StudentView> create(@Valid @RequestBody CreatePersonRequest request) {
        var created = people.createStudent(request);
        return ResponseEntity.created(URI.create("/api/students/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PeopleApiService.StudentView update(@PathVariable("id") Long id,
                                               @Valid @RequestBody StudentForm form) {
        return people.updateStudent(id, form);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        people.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
