package com.placement.app.student;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentRepository studentRepository;

    public StudentController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLACEMENT_OFFICER','RECRUITER','STUDENT')")
    public List<Student> getStudents(Authentication authentication) {
        boolean isStudentRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));

        if (isStudentRole) {
            Student ownProfile = studentRepository.findByUserUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Student profile is not mapped to this account"));
            return List.of(ownProfile);
        }

        return studentRepository.findAll();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public Student getMyProfile(Authentication authentication) {
        return studentRepository.findByUserUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Student profile is not mapped to this account"));
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    public Student createStudent(@Valid @RequestBody Student student) {
        return studentRepository.save(student);
    }
}
