package com.example.fmschool.controller;

import com.example.fmschool.dto.*;
import com.example.fmschool.service.PeopleApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherApiController {
    private final PeopleApiService people;

    public TeacherApiController(PeopleApiService people) { this.people = people; }

    @GetMapping
    public List<PeopleApiService.TeacherView> list() { return people.teachers(); }

    @GetMapping("/{id}")
    public PeopleApiService.TeacherView get(@PathVariable("id") Long id) { return people.teacher(id); }

    @PostMapping
    public ResponseEntity<PeopleApiService.TeacherView> create(@Valid @RequestBody CreatePersonRequest request) {
        var created = people.createTeacher(request);
        return ResponseEntity.created(URI.create("/api/teachers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PeopleApiService.TeacherView update(@PathVariable("id") Long id,
                                               @Valid @RequestBody ProfileForm form) {
        return people.updateTeacher(id, form);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        people.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}
