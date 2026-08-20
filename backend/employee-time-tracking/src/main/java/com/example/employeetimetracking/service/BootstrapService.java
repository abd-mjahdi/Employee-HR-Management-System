package com.example.employeetimetracking.service;

import com.example.employeetimetracking.config.BootstrapProperties;
import com.example.employeetimetracking.config.TenantProperties;
import com.example.employeetimetracking.dto.request.BootstrapCompanyRequestDto;
import com.example.employeetimetracking.dto.response.BootstrapCompanyResponseDto;
import com.example.employeetimetracking.exception.BootstrapAlreadyCompletedException;
import com.example.employeetimetracking.exception.BootstrapDisabledException;
import com.example.employeetimetracking.exception.EmailAlreadyRegisteredException;
import com.example.employeetimetracking.exception.InvalidBootstrapKeyException;
import com.example.employeetimetracking.exception.InvalidSlugException;
import com.example.employeetimetracking.exception.WeakPasswordException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.tenant.TenantResolver;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@Service
public class BootstrapService {

    private static final String DEFAULT_DEPARTMENT_NAME = "General";
    private static final String DEFAULT_DEPARTMENT_CODE = "GEN";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BootstrapProperties bootstrapProperties;
    private final TenantProperties tenantProperties;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public BootstrapService(BootstrapProperties bootstrapProperties,
                            TenantProperties tenantProperties,
                            CompanyRepository companyRepository,
                            DepartmentRepository departmentRepository,
                            UserRepository userRepository,
                            CompanyMembershipRepository companyMembershipRepository,
                            BCryptPasswordEncoder passwordEncoder) {
        this.bootstrapProperties = bootstrapProperties;
        this.tenantProperties = tenantProperties;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public BootstrapCompanyResponseDto bootstrap(String providedKey, BootstrapCompanyRequestDto request) {
        authorize(providedKey);

        String slug = normalizeSlug(request.getSlug());
        if (companyRepository.count() > 0) {
            throw new BootstrapAlreadyCompletedException();
        }

        String email = request.getAdminEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        String rawPassword = resolvePassword(request.getAdminPassword());
        boolean generatedPassword = request.getAdminPassword() == null;

        Company company = new Company();
        company.setName(request.getCompanyName().trim());
        company.setSlug(slug);
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);

        Department department = new Department();
        department.setCompany(company);
        department.setDepartmentName(DEFAULT_DEPARTMENT_NAME);
        department.setDepartmentCode(DEFAULT_DEPARTMENT_CODE);
        department.setIsActive(true);
        department = departmentRepository.save(department);

        User admin = new User();
        admin.setUsername(uniqueUsername(email));
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setFirstName(request.getAdminFirstName().trim());
        admin.setLastName(request.getAdminLastName().trim());
        admin.setIsActive(true);
        admin = userRepository.save(admin);

        CompanyMembership membership = new CompanyMembership();
        membership.setUser(admin);
        membership.setCompany(company);
        membership.setRole(UserRole.HR_ADMIN);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setDepartment(department);
        membership.setManagerMembership(null);
        companyMembershipRepository.save(membership);

        return new BootstrapCompanyResponseDto(
                company.getName(),
                company.getSlug(),
                admin.getEmail(),
                generatedPassword ? rawPassword : null,
                slug + "." + tenantProperties.getBaseDomain()
        );
    }

    private void authorize(String providedKey) {
        if (!bootstrapProperties.isEnabled()) {
            throw new BootstrapDisabledException();
        }
        if (!bootstrapProperties.matches(providedKey)) {
            throw new InvalidBootstrapKeyException();
        }
    }

    private String normalizeSlug(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidSlugException("Invalid slug");
        }
        String slug = raw.trim().toLowerCase(Locale.ROOT);
        if (!TenantResolver.SLUG_PATTERN.matcher(slug).matches() || tenantProperties.reservedSlugSet().contains(slug)) {
            throw new InvalidSlugException("Invalid slug");
        }
        return slug;
    }

    private static String resolvePassword(String provided) {
        if (provided == null) {
            byte[] bytes = new byte[12];
            RANDOM.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
        if (provided.length() < 6) {
            throw new WeakPasswordException("Password must be at least 6 characters");
        }
        return provided;
    }

    private String uniqueUsername(String email) {
        int at = email.indexOf('@');
        String local = (at > 0 ? email.substring(0, at) : email).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (local.isBlank()) {
            local = "admin";
        }
        if (local.length() > 40) {
            local = local.substring(0, 40);
        }
        String candidate = local;
        int suffix = 0;
        while (userRepository.existsByUsername(candidate)) {
            suffix++;
            String extra = String.valueOf(suffix);
            int keep = Math.min(local.length(), 50 - extra.length());
            candidate = local.substring(0, keep) + extra;
        }
        return candidate;
    }
}
