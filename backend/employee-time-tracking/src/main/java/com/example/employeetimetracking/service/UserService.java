package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateUserRequestDto;
import com.example.employeetimetracking.dto.request.UserRequestDto;
import com.example.employeetimetracking.dto.request.UserUpdateDto;
import com.example.employeetimetracking.dto.response.UserCreatedResponse;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.exception.DepartmentNotFoundException;
import com.example.employeetimetracking.exception.EmailAlreadyRegisteredException;
import com.example.employeetimetracking.exception.InvalidEmployeeManagerException;
import com.example.employeetimetracking.exception.InvalidManagerSupervisorException;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.exception.InvalidUserException;
import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.exception.UsernameAlreadyExists;
import com.example.employeetimetracking.mapper.UserMapper;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.specification.UserSpecifications;
import com.example.employeetimetracking.tenant.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder;
    private final UserRepository userRepository;
    private final DepartmentService departmentService;
    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final LeaveBalanceService leaveBalanceService;

    @Autowired
    public UserService(UserRepository userRepository,
                       DepartmentService departmentService,
                       DepartmentRepository departmentRepository,
                       CompanyRepository companyRepository,
                       CompanyMembershipRepository companyMembershipRepository,
                       BCryptPasswordEncoder encoder,
                       UserMapper userMapper,
                       LeaveBalanceService leaveBalanceService) {
        this.userRepository = userRepository;
        this.departmentService = departmentService;
        this.departmentRepository = departmentRepository;
        this.companyRepository = companyRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.encoder = encoder;
        this.userMapper = userMapper;
        this.leaveBalanceService = leaveBalanceService;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public Page<UserResponseDto> getAll(Pageable p) {
        return userRepository.findAll(p).map(userMapper::toDto);
    }

    @Transactional
    public void deactivateUserById(Long id, Long actorId) {
        if (Objects.equals(id, actorId)) {
            throw new InvalidUserException("You cannot deactivate your own account");
        }
        User user = getById(id);
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidUserException("User is already deactivated");
        }
        if (user.getUserRole() == UserRole.HR_ADMIN
                && userRepository.countByUserRoleAndIsActive(UserRole.HR_ADMIN, true) <= 1) {
            throw new InvalidUserException("Cannot deactivate the last active HR admin");
        }
        List<User> activeReports = userRepository.findByManagerIdAndIsActive(id, true);
        if (!activeReports.isEmpty()) {
            throw new InvalidUserException(
                    "Cannot deactivate user who still has active direct reports; reassign them first");
        }
        user.setIsActive(false);
    }

    @Transactional
    public void activateUserById(Long id) {
        User user = getById(id);
        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidUserException("User is already active");
        }
        // Re-validate hierarchy before bringing the account back online
        assertManagerAssignmentValid(id, user.getUserRole(), user.getManager());
        user.setIsActive(true);
    }

    public List<User> getAllByDepartment(Long id, boolean bool) {
        return userRepository.findByDepartmentIdAndIsActive(id, bool);
    }

    /** Prevent enumeration: missing and unauthorized look the same. */
    public UserResponseDto getUserIfAllowed(Long id, CustomUserDetails authenticatedUser) {
        try {
            User targetUser = getById(id);
            Long managerId = targetUser.getManager() != null ? targetUser.getManager().getId() : null;
            boolean isHrAdmin = authenticatedUser.hasRole("HR_ADMIN");
            boolean isManager = Objects.equals(authenticatedUser.getId(), managerId);
            boolean isSameUser = Objects.equals(authenticatedUser.getId(), id);

            if (isHrAdmin || isManager || isSameUser) {
                return userMapper.toDto(targetUser);
            }
            throw new AccessDeniedException("You cannot access this resource");
        } catch (UserNotFoundException | AccessDeniedException e) {
            throw new AccessDeniedException("You cannot access this resource");
        }
    }

    public UserResponseDto getUserIfAllowed(Long id) {
        return userMapper.toDto(getById(id));
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto userRequestDto) {
        User targetUser = getById(id);
        updateAllFields(targetUser, userRequestDto);
        return userMapper.toDto(targetUser);
    }

    public List<UserResponseDto> getTeamMembers(Long id) {
        return userRepository.findByManagerIdAndIsActive(id, true).stream()
                .map(userMapper::toDto)
                .toList();
    }

    private void updateAllFields(User targetUser, UserRequestDto dto) {
        validateUniqueIdentityOnUpdate(dto, targetUser.getId());

        UserRole effectiveRole = dto.getUserRole() != null ? dto.getUserRole() : targetUser.getUserRole();

        if (dto.getUserRole() != null) {
            targetUser.setUserRole(dto.getUserRole());
        }

        if (effectiveRole == UserRole.HR_ADMIN) {
            if (dto.getManagerId() != null) {
                throw new InvalidUserException("HR admin accounts cannot have a manager");
            }
            targetUser.setManager(null);
        } else if (dto.getManagerId() != null) {
            User manager = getById(dto.getManagerId());
            assertManagerAssignmentValid(targetUser.getId(), effectiveRole, manager);
            targetUser.setManager(manager);
        } else if (dto.getUserRole() != null) {
            // Role changed but manager left as-is — re-validate against new role
            assertManagerAssignmentValid(targetUser.getId(), effectiveRole, targetUser.getManager());
        }

        if (dto.getUsername() != null) {
            targetUser.setUsername(dto.getUsername());
        }
        if (dto.getEmail() != null) {
            targetUser.setEmail(dto.getEmail());
        }
        if (dto.getFirstName() != null) {
            targetUser.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            targetUser.setLastName(dto.getLastName());
        }
        if (dto.getDepartmentId() != null) {
            targetUser.setDepartment(departmentService.getById(dto.getDepartmentId()));
        }
    }

    @Transactional
    public UserCreatedResponse createUser(CreateUserRequestDto requestDto) {
        Long companyId = TenantContext.require().companyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new InvalidTenantException("Tenant not found"));

        String email = requestDto.getEmail().trim().toLowerCase(Locale.ROOT);
        Department department = departmentRepository.findByIdAndCompanyId(requestDto.getDepartmentId(), companyId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department does not exist"));
        CompanyMembership managerMembership = resolveManagerMembership(
                requestDto.getManagerMembershipId(), requestDto.getUserRole(), companyId, null);

        Optional<User> existing = userRepository.findByEmail(email);
        boolean createdNewUser = existing.isEmpty();
        String tempPassword = null;
        User user;

        if (createdNewUser) {
            if (userRepository.existsByUsername(requestDto.getUsername())) {
                throw new UsernameAlreadyExists("username unavailable");
            }
            tempPassword = generateTemporaryPassword();
            user = new User();
            user.setUsername(requestDto.getUsername());
            user.setEmail(email);
            user.setPasswordHash(encoder.encode(tempPassword));
            user.setFirstName(requestDto.getFirstName());
            user.setLastName(requestDto.getLastName());
            user.setIsActive(true);
        } else {
            user = existing.get();
            if (companyMembershipRepository.findByUserIdAndCompanyId(user.getId(), companyId).isPresent()) {
                throw new EmailAlreadyRegisteredException("User already belongs to this company");
            }
        }

        dualWriteUser(user, requestDto.getUserRole(), department, managerMembership);
        user = userRepository.save(user);

        CompanyMembership membership = new CompanyMembership();
        membership.setUser(user);
        membership.setCompany(company);
        membership.setRole(requestDto.getUserRole());
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setDepartment(department);
        membership.setManagerMembership(managerMembership);
        companyMembershipRepository.save(membership);

        leaveBalanceService.initializeLeaveBalances(user);
        return new UserCreatedResponse(userMapper.toDto(user), tempPassword);
    }

    private void dualWriteUser(User user,
                               UserRole role,
                               Department department,
                               CompanyMembership managerMembership) {
        user.setUserRole(role);
        user.setDepartment(department);
        user.setManager(managerMembership == null ? null : managerMembership.getUser());
    }

    public CompanyMembership requireManagerMembership(Long managerMembershipId, UserRole userRole, Long companyId) {
        return resolveManagerMembership(managerMembershipId, userRole, companyId, null);
    }

    private CompanyMembership resolveManagerMembership(Long managerMembershipId,
                                                       UserRole userRole,
                                                       Long companyId,
                                                       Long userId) {
        if (userRole == UserRole.HR_ADMIN) {
            if (managerMembershipId != null) {
                throw new InvalidUserException("HR admin accounts cannot have a manager");
            }
            return null;
        }

        if (managerMembershipId == null) {
            if (userRole == UserRole.EMPLOYEE) {
                throw new InvalidEmployeeManagerException("Employee must have a manager with role MANAGER");
            }
            throw new InvalidManagerSupervisorException("Manager must have a supervisor with role HR_ADMIN");
        }

        CompanyMembership managerMembership = companyMembershipRepository
                .findByIdAndCompanyId(managerMembershipId, companyId)
                .orElseThrow(() -> invalidManager(userRole));

        if (managerMembership.getStatus() != MembershipStatus.ACTIVE
                || !Boolean.TRUE.equals(managerMembership.getUser().getIsActive())) {
            throw new InvalidUserException("Assigned manager must be an active user");
        }
        if (userId != null && Objects.equals(userId, managerMembership.getUser().getId())) {
            throw new InvalidUserException("A user cannot be their own manager");
        }
        if (userRole == UserRole.EMPLOYEE && managerMembership.getRole() != UserRole.MANAGER) {
            throw new InvalidEmployeeManagerException("Employee must have a manager with role MANAGER");
        }
        if (userRole == UserRole.MANAGER && managerMembership.getRole() != UserRole.HR_ADMIN) {
            throw new InvalidManagerSupervisorException("Manager must have a supervisor with role HR_ADMIN");
        }
        assertNoMembershipCycle(userId, managerMembership);
        return managerMembership;
    }

    private static RuntimeException invalidManager(UserRole userRole) {
        if (userRole == UserRole.EMPLOYEE) {
            return new InvalidEmployeeManagerException("Employee must have a manager with role MANAGER");
        }
        return new InvalidManagerSupervisorException("Manager must have a supervisor with role HR_ADMIN");
    }

    private void assertNoMembershipCycle(Long userId, CompanyMembership managerMembership) {
        Set<Long> seen = new HashSet<>();
        CompanyMembership current = managerMembership;
        while (current != null) {
            if (!seen.add(current.getId())) {
                throw new InvalidUserException("Manager hierarchy contains a cycle");
            }
            if (userId != null && Objects.equals(current.getUser().getId(), userId)) {
                throw new InvalidUserException("Manager assignment would create a cycle");
            }
            current = current.getManagerMembership();
        }
    }

    /**
     * Hierarchy rules:
     * - EMPLOYEE → active MANAGER supervisor (required)
     * - MANAGER → active HR_ADMIN supervisor (required)
     * - HR_ADMIN → no manager
     * - No self-manager, no cycles, manager must be active
     */
    private void assertManagerAssignmentValid(Long userId, UserRole userRole, User manager) {
        if (userRole == UserRole.HR_ADMIN) {
            if (manager != null) {
                throw new InvalidUserException("HR admin accounts cannot have a manager");
            }
            return;
        }

        if (manager == null) {
            if (userRole == UserRole.EMPLOYEE) {
                throw new InvalidEmployeeManagerException("Employee must have a manager with role MANAGER");
            }
            throw new InvalidManagerSupervisorException("Manager must have a supervisor with role HR_ADMIN");
        }

        if (userId != null && Objects.equals(userId, manager.getId())) {
            throw new InvalidUserException("A user cannot be their own manager");
        }
        if (!Boolean.TRUE.equals(manager.getIsActive())) {
            throw new InvalidUserException("Assigned manager must be an active user");
        }

        if (userRole == UserRole.EMPLOYEE && manager.getUserRole() != UserRole.MANAGER) {
            throw new InvalidEmployeeManagerException("Employee must have a manager with role MANAGER");
        }
        if (userRole == UserRole.MANAGER && manager.getUserRole() != UserRole.HR_ADMIN) {
            throw new InvalidManagerSupervisorException("Manager must have a supervisor with role HR_ADMIN");
        }

        assertNoManagerCycle(userId, manager);
    }

    private void assertNoManagerCycle(Long userId, User manager) {
        if (userId == null) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        User current = manager;
        while (current != null) {
            if (!seen.add(current.getId())) {
                throw new InvalidUserException("Manager hierarchy contains a cycle");
            }
            if (Objects.equals(current.getId(), userId)) {
                throw new InvalidUserException("Manager assignment would create a cycle");
            }
            current = current.getManager();
        }
    }

    private void validateUniqueIdentityOnUpdate(UserRequestDto requestDto, Long userId) {
        if (requestDto.getEmail() != null
                && userRepository.existsByEmailAndIdNot(requestDto.getEmail(), userId)) {
            throw new EmailAlreadyRegisteredException("user already exists with that email");
        }
        if (requestDto.getUsername() != null
                && userRepository.existsByUsernameAndIdNot(requestDto.getUsername(), userId)) {
            throw new UsernameAlreadyExists("username unavailable");
        }
    }

    public List<UserResponseDto> searchUsers(Long departmentId, UserRole role, Boolean active, String name) {
        if (name != null && name.isBlank()) {
            name = null;
        }
        Specification<User> spec = Specification
                .where(UserSpecifications.hasDepartmentId(departmentId))
                .and(UserSpecifications.hasRole(role))
                .and(UserSpecifications.isActive(active))
                .and(UserSpecifications.hasName(name));
        return userRepository.findAll(spec).stream().map(userMapper::toDto).toList();
    }

    @Transactional
    public void updateProfile(Long id, UserUpdateDto userUpdateDto) {
        User user = getById(id);
        if (userUpdateDto.getFirstName() != null) {
            user.setFirstName(userUpdateDto.getFirstName());
        }
        if (userUpdateDto.getLastName() != null) {
            user.setLastName(userUpdateDto.getLastName());
        }
    }
}
