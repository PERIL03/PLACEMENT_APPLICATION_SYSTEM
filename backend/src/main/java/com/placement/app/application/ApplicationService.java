package com.placement.app.application;

import com.placement.app.drive.Drive;
import com.placement.app.drive.DriveRepository;
import com.placement.app.student.Student;
import com.placement.app.student.StudentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final DriveRepository driveRepository;
    private final InterviewSlotRepository interviewSlotRepository;
    private final ApplicationStatusHistoryRepository historyRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            StudentRepository studentRepository,
            DriveRepository driveRepository,
            InterviewSlotRepository interviewSlotRepository,
            ApplicationStatusHistoryRepository historyRepository) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.driveRepository = driveRepository;
        this.interviewSlotRepository = interviewSlotRepository;
        this.historyRepository = historyRepository;
    }

    public DriveApplication applyForAuthenticatedStudent(String username, Long driveId) {
        Student student = studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalStateException("Student profile is not mapped to this account"));

        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new IllegalArgumentException("Drive not found"));

        applicationRepository.findByStudentIdAndDriveId(student.getId(), driveId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Already applied for this drive");
                });

        validateDeadline(drive);

        DriveApplication application = new DriveApplication();
        application.setStudent(student);
        application.setDrive(drive);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedAt(Instant.now());
        DriveApplication saved = applicationRepository.save(application);
        saveHistory(saved, null, ApplicationStatus.APPLIED, username, "Application submitted");
        return saved;
    }

    public List<DriveApplication> getApplicationsByStudent(Long studentId) {
        return applicationRepository.findByStudentId(studentId);
    }

    public List<DriveApplication> autoShortlist(Long driveId, String actorUsername) {
        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new IllegalArgumentException("Drive not found"));

        List<DriveApplication> pendingApplications = applicationRepository.findByDriveIdAndStatus(
                driveId,
                ApplicationStatus.APPLIED);

        for (DriveApplication application : pendingApplications) {
            ApplicationStatus oldStatus = application.getStatus();
            boolean eligible = isEligible(application.getStudent(), drive);
            ApplicationStatus newStatus = eligible ? ApplicationStatus.SHORTLISTED : ApplicationStatus.REJECTED;

            if (oldStatus != newStatus) {
                application.setStatus(newStatus);
                saveHistory(
                        application,
                        oldStatus,
                        newStatus,
                        actorUsername,
                        "Auto-shortlisted by eligibility engine");
            }
        }

        return applicationRepository.saveAll(pendingApplications);
    }

    public DriveApplication updateStatus(Long applicationId, ApplicationStatus newStatus, String actorUsername, String remarks) {
        DriveApplication application = getApplicationById(applicationId);
        ApplicationStatus oldStatus = application.getStatus();

        if (newStatus == ApplicationStatus.INTERVIEW_SCHEDULED) {
            throw new IllegalStateException("Use interview scheduling endpoint for INTERVIEW_SCHEDULED status");
        }

        if (oldStatus == newStatus) {
            throw new IllegalStateException("Application is already in this status");
        }

        validateTransition(oldStatus, newStatus);

        application.setStatus(newStatus);
        DriveApplication saved = applicationRepository.save(application);
        saveHistory(saved, oldStatus, newStatus, actorUsername, remarks);
        return saved;
    }

    public DriveApplication scheduleInterview(
            Long applicationId,
            LocalDateTime scheduledAt,
            String mode,
            String meetingLink,
            String actorUsername) {

        DriveApplication application = getApplicationById(applicationId);

        if (application.getStatus() != ApplicationStatus.SHORTLISTED
                && application.getStatus() != ApplicationStatus.INTERVIEW_SCHEDULED) {
            throw new IllegalStateException("Interview can only be scheduled for shortlisted applications");
        }

        InterviewSlot slot = interviewSlotRepository.findByApplicationId(applicationId)
                .orElseGet(InterviewSlot::new);

        slot.setApplication(application);
        slot.setScheduledAt(scheduledAt);
        slot.setMode(mode);
        slot.setMeetingLink(meetingLink);
        slot.setCreatedBy(actorUsername);
        slot.setCreatedAt(Instant.now());

        InterviewSlot savedSlot = interviewSlotRepository.save(slot);
        application.setInterviewSlot(savedSlot);

        if (application.getStatus() != ApplicationStatus.INTERVIEW_SCHEDULED) {
            ApplicationStatus oldStatus = application.getStatus();
            application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
            saveHistory(
                    application,
                    oldStatus,
                    ApplicationStatus.INTERVIEW_SCHEDULED,
                    actorUsername,
                    "Interview scheduled");
        }

        return applicationRepository.save(application);
    }

    public List<ApplicationStatusHistory> getStatusHistory(Long applicationId) {
        getApplicationById(applicationId);
        return historyRepository.findByApplicationIdOrderByChangedAtDesc(applicationId);
    }

    public DriveApplication getApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
    }

    public Student getStudentProfileForUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalStateException("Student profile is not mapped to this account"));
    }

    private void validateDeadline(Drive drive) {
        if (drive.getApplicationDeadline() != null && LocalDate.now().isAfter(drive.getApplicationDeadline())) {
            throw new IllegalStateException("Application deadline is over");
        }
    }

    private boolean isEligible(Student student, Drive drive) {
        if (drive.getApplicationDeadline() != null && LocalDate.now().isAfter(drive.getApplicationDeadline())) {
            return false;
        }

        if (drive.getMinCgpa() != null && (student.getCgpa() == null || student.getCgpa() < drive.getMinCgpa())) {
            return false;
        }

        String requiredBranch = drive.getEligibleBranch();
        if (requiredBranch != null && !requiredBranch.equalsIgnoreCase("Any")) {
            String studentBranch = student.getBranch() == null ? "" : student.getBranch();
            if (!studentBranch.equalsIgnoreCase(requiredBranch)) {
                return false;
            }
        }

        return true;
    }

    private void validateTransition(ApplicationStatus from, ApplicationStatus to) {
        boolean allowed = switch (from) {
            case APPLIED -> to == ApplicationStatus.SHORTLISTED || to == ApplicationStatus.REJECTED;
            case SHORTLISTED -> to == ApplicationStatus.INTERVIEW_SCHEDULED || to == ApplicationStatus.REJECTED;
            case INTERVIEW_SCHEDULED -> to == ApplicationStatus.OFFERED || to == ApplicationStatus.REJECTED;
            case OFFERED, REJECTED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException("Invalid status transition from " + from + " to " + to);
        }
    }

    private void saveHistory(
            DriveApplication application,
            ApplicationStatus oldStatus,
            ApplicationStatus newStatus,
            String changedBy,
            String remarks) {

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setRemarks(remarks);
        history.setChangedAt(Instant.now());
        historyRepository.save(history);
    }
}
