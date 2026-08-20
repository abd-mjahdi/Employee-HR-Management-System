package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService {

    private final CompanyMembershipRepository companyMembershipRepository;

    public CustomUserDetailsService(CompanyMembershipRepository companyMembershipRepository) {
        this.companyMembershipRepository = companyMembershipRepository;
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadForTenant(String email, Long companyId) {
        CompanyMembership membership = companyMembershipRepository
                .findByCompanyIdAndUserEmail(companyId, email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return new CustomUserDetails(membership);
    }
}
