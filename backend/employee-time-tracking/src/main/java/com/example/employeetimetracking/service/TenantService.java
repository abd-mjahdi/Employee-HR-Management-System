package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.InactiveTenantException;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.repository.CompanyRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantService {
    private static final String UNKNOWN_TENANT = "Tenant not found";

    private final CompanyRepository companyRepository;

    public TenantService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company requireActiveBySlug(String slug) {
        Company company = companyRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new InvalidTenantException(UNKNOWN_TENANT));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new InactiveTenantException(UNKNOWN_TENANT);
        }
        return company;
    }
}
