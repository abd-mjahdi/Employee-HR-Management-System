package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.LoginRequestDto;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.exception.AccountDeactivatedException;
import com.example.employeetimetracking.exception.InvalidCredentialsException;
import com.example.employeetimetracking.exception.MembershipInactiveException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.JwtUtil;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {
    private static final String INVALID_CREDENTIALS = "invalid credentials";

    private final UserRepository userRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public LoginService(UserRepository userRepository,
                        CompanyMembershipRepository companyMembershipRepository,
                        JwtUtil jwtUtil,
                        BCryptPasswordEncoder passwordEncoder,
                        LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto requestDto, String clientIp) {
        Long companyId = TenantContext.require().companyId();
        loginAttemptService.assertNotLimited(companyId, clientIp);

        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElse(null);
        if (user == null) {
            loginAttemptService.recordFailure(companyId, clientIp, "unknown_user");
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(companyId, clientIp, "bad_password");
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        CompanyMembership membership = companyMembershipRepository
                .findByUserIdAndCompanyId(user.getId(), companyId)
                .orElse(null);
        if (membership == null) {
            loginAttemptService.recordFailure(companyId, clientIp, "no_membership");
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            loginAttemptService.recordFailure(companyId, clientIp, "inactive_user");
            throw new AccountDeactivatedException("Account deactivated");
        }
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            loginAttemptService.recordFailure(companyId, clientIp, "inactive_membership");
            throw new MembershipInactiveException("Account deactivated");
        }

        loginAttemptService.recordSuccess(companyId, clientIp);

        Company company = membership.getCompany();
        String token = jwtUtil.generateJwtToken(
                user.getEmail(),
                user.getId(),
                company.getId(),
                membership.getId(),
                membership.getRole()
        );

        return new LoginResponseDto(
                token,
                user.getEmail(),
                membership.getRole(),
                company.getSlug(),
                company.getName()
        );
    }
}
