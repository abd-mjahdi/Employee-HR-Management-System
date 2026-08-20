package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, Long> {
    Optional<CompanyMembership> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
            SELECT m FROM CompanyMembership m
            JOIN FETCH m.user u
            JOIN FETCH m.company c
            WHERE u.id = :userId AND c.id = :companyId
            """)
    Optional<CompanyMembership> findByUserIdAndCompanyId(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT m FROM CompanyMembership m
            JOIN FETCH m.user u
            JOIN FETCH m.company c
            WHERE m.id = :id AND c.id = :companyId AND u.id = :userId
            """)
    Optional<CompanyMembership> findByIdAndCompanyIdAndUserId(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("userId") Long userId);

    List<CompanyMembership> findByCompanyIdAndStatus(Long companyId, MembershipStatus status);

    @Query("""
            SELECT m FROM CompanyMembership m
            JOIN FETCH m.user u
            JOIN FETCH m.company c
            WHERE c.id = :companyId AND lower(u.email) = lower(:email)
            """)
    Optional<CompanyMembership> findByCompanyIdAndUserEmail(
            @Param("companyId") Long companyId,
            @Param("email") String email);

    long countByCompanyIdAndRoleAndStatus(Long companyId, UserRole role, MembershipStatus status);

    List<CompanyMembership> findByManagerMembershipIdAndStatus(Long managerMembershipId, MembershipStatus status);

    Page<CompanyMembership> findByCompanyId(Long companyId, Pageable pageable);

    List<CompanyMembership> findByCompanyIdAndDepartmentIdAndStatus(
            Long companyId, Long departmentId, MembershipStatus status);
}
