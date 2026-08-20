package com.example.employeetimetracking.tenant;

import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MembershipAccess {
    private final CompanyMembershipRepository companyMembershipRepository;

    public MembershipAccess(CompanyMembershipRepository companyMembershipRepository) {
        this.companyMembershipRepository = companyMembershipRepository;
    }

    public Long resolveCompanyId(CustomUserDetails authenticatedUser) {
        if (authenticatedUser != null && authenticatedUser.getCompanyId() != null) {
            return authenticatedUser.getCompanyId();
        }
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new InvalidTenantException("Tenant not found");
        }
        return companyId;
    }

    public Optional<CompanyMembership> find(Long userId, Long companyId) {
        if (userId == null || companyId == null) {
            return Optional.empty();
        }
        return companyMembershipRepository.findByUserIdAndCompanyId(userId, companyId);
    }

    public Optional<CompanyMembership> findInCurrentCompany(Long userId) {
        return find(userId, TenantContext.getCompanyId());
    }

    public Optional<CompanyMembership> findFor(CustomUserDetails authenticatedUser, Long userId) {
        return find(userId, resolveCompanyId(authenticatedUser));
    }

    public boolean isHrAdmin(Long userId) {
        return findInCurrentCompany(userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .map(m -> m.getRole() == UserRole.HR_ADMIN)
                .orElse(false);
    }

    public boolean isDirectManagerOf(Long actorUserId, Long targetUserId) {
        if (actorUserId == null || targetUserId == null) {
            return false;
        }
        CompanyMembership target = findInCurrentCompany(targetUserId).orElse(null);
        if (target == null
                || target.getStatus() != MembershipStatus.ACTIVE
                || target.getManagerMembership() == null
                || target.getManagerMembership().getUser() == null) {
            return false;
        }
        return actorUserId.equals(target.getManagerMembership().getUser().getId());
    }

    public Long managerUserId(Long targetUserId) {
        return findInCurrentCompany(targetUserId)
                .map(CompanyMembership::getManagerMembership)
                .map(CompanyMembership::getUser)
                .map(user -> user.getId())
                .orElse(null);
    }

    public Map<Long, CompanyMembership> mapByUserId(Long companyId, Collection<Long> userIds) {
        if (companyId == null || userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return companyMembershipRepository.findByCompanyIdAndUserIdIn(companyId, userIds).stream()
                .filter(m -> m.getUser() != null && m.getUser().getId() != null)
                .collect(Collectors.toMap(m -> m.getUser().getId(), m -> m, (a, b) -> a));
    }

    public Department departmentOf(CompanyMembership membership) {
        return membership == null ? null : membership.getDepartment();
    }
}
