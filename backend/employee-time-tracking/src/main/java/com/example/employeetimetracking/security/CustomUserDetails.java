package com.example.employeetimetracking.security;

import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    @Getter
    private final Long id;
    @Getter
    private final String email;
    @Getter
    private final Long companyId;
    @Getter
    private final Long membershipId;
    @Getter
    private final UserRole role;
    @Getter
    private final MembershipStatus membershipStatus;
    private final String password;
    private final boolean active;
    private final Collection<? extends SimpleGrantedAuthority> authorities;

    public CustomUserDetails(CompanyMembership membership) {
        User user = membership.getUser();
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.active = Boolean.TRUE.equals(user.getIsActive());
        this.companyId = membership.getCompany().getId();
        this.membershipId = membership.getId();
        this.role = membership.getRole();
        this.membershipStatus = membership.getStatus();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + membership.getRole().name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public boolean hasRole(String roleName) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName));
    }
}
