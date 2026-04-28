package com.placement.app.company;

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
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLACEMENT_OFFICER','RECRUITER','STUDENT')")
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('PLACEMENT_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    public Company createCompany(@RequestBody Company company) {
        return companyRepository.save(company);
    }
}
