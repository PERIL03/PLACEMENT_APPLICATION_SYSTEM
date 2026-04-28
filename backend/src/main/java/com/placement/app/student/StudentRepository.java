package com.placement.app.student;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

	Optional<Student> findByUserUsername(String username);

	Optional<Student> findByEmail(String email);

	List<Student> findAllByEmailOrderByIdAsc(String email);
}
