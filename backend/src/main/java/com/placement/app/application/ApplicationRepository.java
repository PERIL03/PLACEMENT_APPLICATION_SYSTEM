package com.placement.app.application;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<DriveApplication, Long> {

    Optional<DriveApplication> findByStudentIdAndDriveId(Long studentId, Long driveId);

    List<DriveApplication> findByStudentId(Long studentId);

    List<DriveApplication> findByDriveIdAndStatus(Long driveId, ApplicationStatus status);
}
