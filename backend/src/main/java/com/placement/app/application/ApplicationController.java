package com.placement.app.application;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@RequestBody ApplyRequest request, Authentication authentication) {
        DriveApplication application = applicationService.applyForAuthenticatedStudent(
                authentication.getName(),
                request.driveId());
        return ApplicationResponse.from(application);
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT','PLACEMENT_OFFICER','RECRUITER')")
    public List<ApplicationResponse> getApplicationsByStudent(@PathVariable Long studentId, Authentication authentication) {
        boolean isStudentRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));

        if (isStudentRole) {
            Long ownStudentId = applicationService.getStudentProfileForUsername(authentication.getName()).getId();
            if (!ownStudentId.equals(studentId)) {
                throw new AccessDeniedException("Students can only view their own applications");
            }
        }

        return applicationService.getApplicationsByStudent(studentId).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @PostMapping("/drives/{driveId}/auto-shortlist")
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    public List<ApplicationResponse> autoShortlist(@PathVariable Long driveId, Authentication authentication) {
        return applicationService.autoShortlist(driveId, authentication.getName()).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @PostMapping("/{applicationId}/status")
    @PreAuthorize("hasAnyRole('PLACEMENT_OFFICER','RECRUITER')")
    public ApplicationResponse updateStatus(
            @PathVariable Long applicationId,
            @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        DriveApplication updated = applicationService.updateStatus(
                applicationId,
                request.status(),
                authentication.getName(),
                request.remarks());
        return ApplicationResponse.from(updated);
    }

    @PostMapping("/{applicationId}/interview-slot")
    @PreAuthorize("hasAnyRole('PLACEMENT_OFFICER','RECRUITER')")
    public ApplicationResponse scheduleInterview(
            @PathVariable Long applicationId,
            @RequestBody ScheduleInterviewRequest request,
            Authentication authentication) {
        DriveApplication updated = applicationService.scheduleInterview(
                applicationId,
                request.scheduledAt(),
                request.mode(),
                request.meetingLink(),
                authentication.getName());
        return ApplicationResponse.from(updated);
    }

    @GetMapping("/{applicationId}/history")
    @PreAuthorize("hasAnyRole('STUDENT','PLACEMENT_OFFICER','RECRUITER')")
    public List<ApplicationHistoryResponse> getHistory(@PathVariable Long applicationId, Authentication authentication) {
        assertStudentOwnership(authentication, applicationId);

        return applicationService.getStatusHistory(applicationId).stream()
                .map(ApplicationHistoryResponse::from)
                .toList();
    }

    private void assertStudentOwnership(Authentication authentication, Long applicationId) {
        boolean isStudentRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));

        if (!isStudentRole) {
            return;
        }

        Long ownStudentId = applicationService.getStudentProfileForUsername(authentication.getName()).getId();
        DriveApplication application = applicationService.getApplicationById(applicationId);
        if (!ownStudentId.equals(application.getStudent().getId())) {
            throw new AccessDeniedException("Students can only view history for their own applications");
        }
    }

    public record ApplyRequest(Long driveId) {
    }

    public record UpdateStatusRequest(ApplicationStatus status, String remarks) {
    }

    public record ScheduleInterviewRequest(LocalDateTime scheduledAt, String mode, String meetingLink) {
    }

    public record ApplicationResponse(
            Long id,
            Long studentId,
            String studentName,
            Long driveId,
            String companyName,
            String roleTitle,
            ApplicationStatus status,
            String appliedAt,
            String interviewAt,
            String interviewMode,
            String interviewLink) {

        public static ApplicationResponse from(DriveApplication application) {
            InterviewSlot interviewSlot = application.getInterviewSlot();

            return new ApplicationResponse(
                    application.getId(),
                    application.getStudent().getId(),
                    application.getStudent().getFullName(),
                    application.getDrive().getId(),
                    application.getDrive().getCompany().getName(),
                    application.getDrive().getRoleTitle(),
                    application.getStatus(),
                    application.getAppliedAt().toString(),
                    interviewSlot == null || interviewSlot.getScheduledAt() == null
                            ? null
                            : interviewSlot.getScheduledAt().toString(),
                    interviewSlot == null ? null : interviewSlot.getMode(),
                    interviewSlot == null ? null : interviewSlot.getMeetingLink());
        }
    }

    public record ApplicationHistoryResponse(
            Long id,
            ApplicationStatus oldStatus,
            ApplicationStatus newStatus,
            String changedBy,
            String remarks,
            String changedAt) {

        public static ApplicationHistoryResponse from(ApplicationStatusHistory history) {
            return new ApplicationHistoryResponse(
                    history.getId(),
                    history.getOldStatus(),
                    history.getNewStatus(),
                    history.getChangedBy(),
                    history.getRemarks(),
                    history.getChangedAt().toString());
        }
    }
}
