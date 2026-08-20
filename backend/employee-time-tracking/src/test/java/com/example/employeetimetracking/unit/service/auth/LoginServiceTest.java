package com.example.employeetimetracking.unit.service.auth;

import com.example.employeetimetracking.dto.request.LoginRequestDto;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.exception.AccountDeactivatedException;
import com.example.employeetimetracking.exception.InvalidCredentialsException;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.exception.LoginRateLimitedException;
import com.example.employeetimetracking.exception.MembershipInactiveException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.JwtUtil;
import com.example.employeetimetracking.service.LoginAttemptService;
import com.example.employeetimetracking.service.LoginService;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String IP = "127.0.0.1";

    @Mock UserRepository userRepository;
    @Mock CompanyMembershipRepository companyMembershipRepository;
    @Mock JwtUtil jwtUtil;
    @Mock BCryptPasswordEncoder passwordEncoder;
    @Mock LoginAttemptService loginAttemptService;

    @InjectMocks
    LoginService loginService;

    private User user;
    private Company company;
    private CompanyMembership membership;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        user = new User();
        user.setId(10L);
        user.setEmail("alice.morgan@company.com");
        user.setPasswordHash("hash");
        user.setIsActive(true);

        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setName("Acme");
        company.setStatus(CompanyStatus.ACTIVE);

        membership = new CompanyMembership();
        membership.setId(10L);
        membership.setUser(user);
        membership.setCompany(company);
        membership.setRole(UserRole.HR_ADMIN);
        membership.setStatus(MembershipStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void login_success_issuesTenantBoundJwt() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(companyMembershipRepository.findByUserIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(membership));
        when(jwtUtil.generateJwtToken(user.getEmail(), 10L, 1L, 10L, UserRole.HR_ADMIN)).thenReturn("jwt");

        LoginResponseDto response = loginService.login(new LoginRequestDto(user.getEmail(), "secret"), IP);

        assertTrue(response.isSuccess());
        assertEquals("jwt", response.getToken());
        assertEquals(UserRole.HR_ADMIN, response.getRole());
        assertEquals("acme", response.getCompanySlug());
        assertEquals("Acme", response.getCompanyName());
        verify(jwtUtil).generateJwtToken(user.getEmail(), 10L, 1L, 10L, UserRole.HR_ADMIN);
        verify(loginAttemptService).recordSuccess(1L, IP);
        verify(loginAttemptService, never()).recordFailure(any(), any(), any());
    }

    @Test
    void login_withoutTenantContext_fails() {
        TenantContext.clear();
        assertThrows(InvalidTenantException.class,
                () -> loginService.login(new LoginRequestDto(user.getEmail(), "secret"), IP));
        verify(loginAttemptService, never()).recordFailure(any(), any(), any());
    }

    @Test
    void login_success_jwtContainsMatchingCompanyId() {
        JwtUtil realJwt = new JwtUtil();
        ReflectionTestUtils.setField(realJwt, "expirationDuration", 3_600_000L);
        ReflectionTestUtils.setField(
                realJwt,
                "jwtSecret",
                "0ef2d553cf17c6a144e82d71ca3d5d1f931f4b42d637a6d0b3ae645be2e1e67a"
        );
        LoginService service = new LoginService(
                userRepository, companyMembershipRepository, realJwt, passwordEncoder, loginAttemptService);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(companyMembershipRepository.findByUserIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(membership));

        LoginResponseDto response = service.login(new LoginRequestDto(user.getEmail(), "secret"), IP);

        assertEquals(1L, realJwt.extractCompanyId(response.getToken()));
        assertEquals(10L, realJwt.extractUserId(response.getToken()));
        assertEquals(10L, realJwt.extractMembershipId(response.getToken()));
        assertEquals(user.getEmail(), realJwt.extractEmail(response.getToken()));
        assertEquals("HR_ADMIN", realJwt.extractRole(response.getToken()));
    }

    @Test
    void login_validPasswordWrongEmail_isInvalidCredentials() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class,
                () -> loginService.login(new LoginRequestDto("nope@x.com", "secret"), IP));
        verify(loginAttemptService).recordFailure(1L, IP, "unknown_user");
    }

    @Test
    void login_wrongPassword_isInvalidCredentials() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class,
                () -> loginService.login(new LoginRequestDto(user.getEmail(), "bad"), IP));
        verify(loginAttemptService).recordFailure(1L, IP, "bad_password");
    }

    @Test
    void login_noMembershipInTenant_isInvalidCredentials() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(companyMembershipRepository.findByUserIdAndCompanyId(10L, 1L)).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class,
                () -> loginService.login(new LoginRequestDto(user.getEmail(), "secret"), IP));
        verify(loginAttemptService).recordFailure(1L, IP, "no_membership");
    }

    @Test
    void login_inactiveUser_isForbiddenWithoutLeakingCompany() {
        user.setIsActive(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(companyMembershipRepository.findByUserIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(membership));
        assertThrows(AccountDeactivatedException.class,
                () -> loginService.login(new LoginRequestDto(user.getEmail(), "secret"), IP));
        verify(loginAttemptService).recordFailure(1L, IP, "inactive_user");
    }

    @Test
    void login_inactiveMembership_isForbiddenWithoutLeakingCompany() {
        membership.setStatus(MembershipStatus.DEACTIVATED);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(companyMembershipRepository.findByUserIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(membership));
        assertThrows(MembershipInactiveException.class,
                () -> loginService.login(new LoginRequestDto(user.getEmail(), "secret"), IP));
        verify(loginAttemptService).recordFailure(eq(1L), eq(IP), eq("inactive_membership"));
    }

    @Test
    void login_rateLimited_doesNotCheckPassword() {
        doThrow(new LoginRateLimitedException("Too many login attempts"))
                .when(loginAttemptService).assertNotLimited(1L, IP);

        assertThrows(LoginRateLimitedException.class,
                () -> loginService.login(new LoginRequestDto(user.getEmail(), "secret"), IP));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void loginRequestDto_toString_omitsPassword() {
        String text = new LoginRequestDto(user.getEmail(), "super-secret").toString();
        assertTrue(text.contains(user.getEmail()));
        assertFalse(text.contains("super-secret"));
    }
}
