package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, Long> {
    Optional<CompanyMembership> findByIdAndCompanyId(Long id, Long companyId);

    Optional<CompanyMembership> findByUserIdAndCompanyId(Long userId, Long companyId);

    List<CompanyMembership> findByCompanyIdAndStatus(Long companyId, MembershipStatus status);

    Optional<CompanyMembership> findByCompanyIdAndUserEmail(Long companyId, String email);

    long countByCompanyIdAndRoleAndStatus(Long companyId, UserRole role, MembershipStatus status);

    List<CompanyMembership> findByManagerMembershipIdAndStatus(Long managerMembershipId, MembershipStatus status);

    Page<CompanyMembership> findByCompanyId(Long companyId, Pageable pageable);

    List<CompanyMembership> findByCompanyIdAndDepartmentIdAndStatus(
            Long companyId, Long departmentId, MembershipStatus status);
}
