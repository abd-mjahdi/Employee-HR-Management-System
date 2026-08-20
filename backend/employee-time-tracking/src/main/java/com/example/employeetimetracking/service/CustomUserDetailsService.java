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

    /**
     * Load the JWT membership in the Host-resolved company. Do not look up "any membership
     * for this email in the current company" — that would accept a token issued for another slug.
     */
    @Transactional(readOnly = true)
    public CustomUserDetails loadForTenant(Long membershipId, Long companyId, Long userId) {
        CompanyMembership membership = companyMembershipRepository
                .findByIdAndCompanyIdAndUserId(membershipId, companyId, userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return new CustomUserDetails(membership);
    }
}
