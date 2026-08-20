package com.example.employeetimetracking.mapper;

import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    private final MembershipAccess membershipAccess;

    public UserMapper(MembershipAccess membershipAccess) {
        this.membershipAccess = membershipAccess;
    }

    public UserResponseDto toDto(User user) {
        CompanyMembership membership = null;
        Long companyId = TenantContext.getCompanyId();
        if (user != null && user.getId() != null && companyId != null) {
            membership = membershipAccess.find(user.getId(), companyId).orElse(null);
        }
        return toDto(user, membership);
    }

    public UserResponseDto toDto(User user, CompanyMembership membership) {
        Long departmentId = null;
        Long managerId = null;
        if (membership != null) {
            if (membership.getDepartment() != null) {
                departmentId = membership.getDepartment().getId();
            }
            if (membership.getManagerMembership() != null
                    && membership.getManagerMembership().getUser() != null) {
                managerId = membership.getManagerMembership().getUser().getId();
            }
        }
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                membership != null ? membership.getRole() : null,
                departmentId,
                managerId,
                user.getIsActive()
        );
    }
}
