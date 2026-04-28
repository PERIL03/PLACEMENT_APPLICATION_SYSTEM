package com.placement.app.drive;

import com.placement.app.company.Company;
import com.placement.app.company.CompanyRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drives")
public class DriveController {

    private final DriveRepository driveRepository;
    private final CompanyRepository companyRepository;

    public DriveController(DriveRepository driveRepository, CompanyRepository companyRepository) {
        this.driveRepository = driveRepository;
        this.companyRepository = companyRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLACEMENT_OFFICER','RECRUITER','STUDENT')")
    public List<DriveResponse> getAllDrives() {
        return driveRepository.findAll().stream()
                .map(DriveResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    public DriveResponse createDrive(@RequestBody CreateDriveRequest request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        Drive drive = new Drive();
        drive.setCompany(company);
        drive.setRoleTitle(request.roleTitle());
        drive.setEligibleBranch(request.eligibleBranch());
        drive.setMinCgpa(request.minCgpa());
        drive.setPackageLpa(request.packageLpa());
        drive.setApplicationDeadline(request.applicationDeadline());

        Drive saved = driveRepository.save(drive);
        return DriveResponse.from(saved);
    }

    public record CreateDriveRequest(
            Long companyId,
            String roleTitle,
            String eligibleBranch,
            Double minCgpa,
            Double packageLpa,
            LocalDate applicationDeadline) {
    }

    public record DriveResponse(
            Long id,
            Long companyId,
            String companyName,
            String roleTitle,
            String eligibleBranch,
            Double minCgpa,
            Double packageLpa,
            LocalDate applicationDeadline) {

        public static DriveResponse from(Drive drive) {
            return new DriveResponse(
                    drive.getId(),
                    drive.getCompany().getId(),
                    drive.getCompany().getName(),
                    drive.getRoleTitle(),
                    drive.getEligibleBranch(),
                    drive.getMinCgpa(),
                    drive.getPackageLpa(),
                    drive.getApplicationDeadline());
        }
    }
}
