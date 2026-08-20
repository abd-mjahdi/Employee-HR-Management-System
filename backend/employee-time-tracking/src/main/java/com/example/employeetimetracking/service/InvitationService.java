package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.AcceptInvitationRequestDto;
import com.example.employeetimetracking.dto.request.CreateInvitationRequestDto;
import com.example.employeetimetracking.dto.response.InvitationAcceptedResponseDto;
import com.example.employeetimetracking.dto.response.InvitationCreatedResponseDto;
import com.example.employeetimetracking.exception.DepartmentNotFoundException;
import com.example.employeetimetracking.exception.EmailAlreadyRegisteredException;
import com.example.employeetimetracking.exception.InvitationAlreadyUsedException;
import com.example.employeetimetracking.exception.InvitationExpiredException;
import com.example.employeetimetracking.exception.InvitationNotFoundException;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.exception.InvalidUserException;
import com.example.employeetimetracking.exception.WeakPasswordException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.Invitation;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.InvitationStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.repository.InvitationRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class InvitationService {

    private static final int EXPIRY_HOURS = 72;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final LeaveBalanceService leaveBalanceService;
    private final BCryptPasswordEncoder passwordEncoder;

    public InvitationService(InvitationRepository invitationRepository,
                             CompanyRepository companyRepository,
                             DepartmentRepository departmentRepository,
                             CompanyMembershipRepository companyMembershipRepository,
                             UserRepository userRepository,
                             UserService userService,
                             LeaveBalanceService leaveBalanceService,
                             BCryptPasswordEncoder passwordEncoder) {
        this.invitationRepository = invitationRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.leaveBalanceService = leaveBalanceService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public InvitationCreatedResponseDto create(CreateInvitationRequestDto request, CustomUserDetails actor) {
        Long companyId = TenantContext.require().companyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new InvalidTenantException("Tenant not found"));

        if (actor.getMembershipId() == null) {
            throw new AccessDeniedException("You cannot access this resource");
        }
        CompanyMembership inviter = companyMembershipRepository
                .findByIdAndCompanyId(actor.getMembershipId(), companyId)
                .orElseThrow(() -> new AccessDeniedException("You cannot access this resource"));

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Department department = departmentRepository.findByIdAndCompanyId(request.getDepartmentId(), companyId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department does not exist"));
        CompanyMembership managerMembership = userService.requireManagerMembership(
                request.getManagerMembershipId(), request.getRole(), companyId);

        userRepository.findByEmail(email).ifPresent(user -> {
            if (companyMembershipRepository.findByUserIdAndCompanyId(user.getId(), companyId).isPresent()) {
                throw new EmailAlreadyRegisteredException("User already belongs to this company");
            }
        });

        invitationRepository.findByCompanyIdAndEmailAndStatus(companyId, email, InvitationStatus.PENDING)
                .forEach(previous -> previous.setStatus(InvitationStatus.REVOKED));

        String rawToken = generateToken();
        Invitation invitation = new Invitation();
        invitation.setCompany(company);
        invitation.setEmail(email);
        invitation.setRole(request.getRole());
        invitation.setDepartment(department);
        invitation.setManagerMembership(managerMembership);
        invitation.setTokenHash(hashToken(rawToken));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedByMembership(inviter);
        invitation.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(EXPIRY_HOURS));
        invitation = invitationRepository.save(invitation);

        return new InvitationCreatedResponseDto(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getExpiresAt(),
                rawToken
        );
    }

    @Transactional
    public InvitationAcceptedResponseDto accept(AcceptInvitationRequestDto request) {
        Long companyId = TenantContext.require().companyId();
        Invitation invitation = invitationRepository.findWithDetailsByTokenHash(hashToken(request.getToken()))
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));

        if (!invitation.getCompany().getId().equals(companyId)) {
            throw new InvitationNotFoundException("Invitation not found");
        }
        if (invitation.getStatus() == InvitationStatus.ACCEPTED
                || invitation.getStatus() == InvitationStatus.REVOKED) {
            throw new InvitationAlreadyUsedException("Invitation is no longer valid");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING
                || invitation.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            throw new InvitationExpiredException("Invitation has expired");
        }

        Optional<User> existing = userRepository.findByEmail(invitation.getEmail());
        User user;
        if (existing.isEmpty()) {
            if (request.getPassword() == null) {
                throw new WeakPasswordException("Password must be at least 6 characters");
            }
            if (blank(request.getFirstName()) || blank(request.getLastName())) {
                throw new InvalidUserException("First name and last name are required");
            }
            user = new User();
            user.setUsername(uniqueUsername(invitation.getEmail()));
            user.setEmail(invitation.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName().trim());
            user.setLastName(request.getLastName().trim());
            user.setIsActive(true);
        } else {
            user = existing.get();
            if (companyMembershipRepository.findByUserIdAndCompanyId(user.getId(), companyId).isPresent()) {
                throw new EmailAlreadyRegisteredException("User already belongs to this company");
            }
        }

        user = userRepository.save(user);

        CompanyMembership membership = new CompanyMembership();
        membership.setUser(user);
        membership.setCompany(invitation.getCompany());
        membership.setRole(invitation.getRole());
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setDepartment(invitation.getDepartment());
        membership.setManagerMembership(invitation.getManagerMembership());
        companyMembershipRepository.save(membership);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now(ZoneOffset.UTC));
        invitation.setAcceptedUser(user);

        leaveBalanceService.initializeLeaveBalances(user);

        return new InvitationAcceptedResponseDto(
                user.getEmail(),
                invitation.getCompany().getSlug(),
                "Invitation accepted"
        );
    }

    @Transactional
    public void revoke(Long invitationId) {
        Long companyId = TenantContext.require().companyId();
        Invitation invitation = invitationRepository.findByIdAndCompanyId(invitationId, companyId)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found"));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationAlreadyUsedException("Invitation is no longer valid");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String uniqueUsername(String email) {
        int at = email.indexOf('@');
        String local = (at > 0 ? email.substring(0, at) : email).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (local.isBlank()) {
            local = "user";
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
