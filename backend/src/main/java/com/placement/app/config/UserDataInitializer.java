package com.placement.app.config;

import com.placement.app.student.Student;
import com.placement.app.student.StudentRepository;
import com.placement.app.user.AppUser;
import com.placement.app.user.AppUserRepository;
import com.placement.app.user.UserRole;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class UserDataInitializer {

    @Bean
    CommandLineRunner seedUsers(
            AppUserRepository appUserRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            createUserIfMissing(appUserRepository, passwordEncoder, "student1", "student123", UserRole.STUDENT);
            createUserIfMissing(appUserRepository, passwordEncoder, "student2", "student234", UserRole.STUDENT);
            createUserIfMissing(appUserRepository, passwordEncoder, "officer1", "officer123", UserRole.PLACEMENT_OFFICER);
            createUserIfMissing(appUserRepository, passwordEncoder, "recruiter1", "recruiter123", UserRole.RECRUITER);

            mapStudentProfile(appUserRepository, studentRepository, "student1", "aarav.m@college.edu");
            mapStudentProfile(appUserRepository, studentRepository, "student2", "riya.s@college.edu");
        };
    }

    private void createUserIfMissing(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String rawPassword,
            UserRole role) {

        if (appUserRepository.existsByUsername(username)) {
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        appUserRepository.save(user);
    }

    private void mapStudentProfile(
            AppUserRepository appUserRepository,
            StudentRepository studentRepository,
            String username,
            String studentEmail) {

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found for mapping: " + username));

        List<Student> candidates = studentRepository.findAllByEmailOrderByIdAsc(studentEmail);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Student not found for mapping: " + studentEmail);
        }

        Student student = candidates.getFirst();

        if (student.getUser() == null || !student.getUser().getId().equals(user.getId())) {
            student.setUser(user);
            studentRepository.save(student);
        }
    }
}
