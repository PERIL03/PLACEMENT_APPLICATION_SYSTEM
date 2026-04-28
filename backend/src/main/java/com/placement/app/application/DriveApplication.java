package com.placement.app.application;

import com.placement.app.drive.Drive;
import com.placement.app.student.Student;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.Instant;

@Entity
public class DriveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "drive_id", nullable = false)
    private Drive drive;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private Instant appliedAt;

    @OneToOne(mappedBy = "application", fetch = FetchType.EAGER)
    private InterviewSlot interviewSlot;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Drive getDrive() {
        return drive;
    }

    public void setDrive(Drive drive) {
        this.drive = drive;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public InterviewSlot getInterviewSlot() {
        return interviewSlot;
    }

    public void setInterviewSlot(InterviewSlot interviewSlot) {
        this.interviewSlot = interviewSlot;
    }
}
