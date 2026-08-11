package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateUserRequestDto;
import com.example.employeetimetracking.dto.request.UserRequestDto;
import com.example.employeetimetracking.dto.request.UserUpdateDto;
import com.example.employeetimetracking.dto.response.UserCreatedResponse;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.exception.EmailAlreadyRegisteredException;
import com.example.employeetimetracking.exception.InvalidEmployeeManagerException;
import com.example.employeetimetracking.exception.InvalidManagerSupervisorException;
import com.example.employeetimetracking.exception.InvalidUserException;
import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.exception.UsernameAlreadyExists;
import com.example.employeetimetracking.mapper.UserMapper;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import com.example.employeetimetracking.specification.UserSpecifications;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder;
    private final UserRepository userRepository;
    private final DepartmentService departmentService;
    private final LeaveBalanceService leaveBalanceService;

    @Autowired
    public UserService(UserRepository userRepository,
                       DepartmentService departmentService,
                       BCryptPasswordEncoder encoder,
                       UserMapper userMapper,
                       LeaveBalanceService leaveBalanceService) {
        this.userRepository = userRepository;
        this.departmentService = departmentService;
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
        validateNewUserData(requestDto);
        String tempPassword = generateTemporaryPassword();
        User user = createUserEntity(requestDto, tempPassword);
        User savedUser = userRepository.save(user);
        leaveBalanceService.initializeLeaveBalances(savedUser);
        return new UserCreatedResponse(userMapper.toDto(savedUser), tempPassword);
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

    private User createUserEntity(CreateUserRequestDto requestDto, String tempPassword) {
        User user = new User();
        user.setUsername(requestDto.getUsername());
        user.setEmail(requestDto.getEmail());
        user.setPasswordHash(encoder.encode(tempPassword));
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());
        user.setUserRole(requestDto.getUserRole());
        user.setDepartment(departmentService.getById(requestDto.getDepartmentId()));
        user.setIsActive(true);

        if (requestDto.getUserRole() == UserRole.HR_ADMIN) {
            if (requestDto.getManagerId() != null) {
                throw new InvalidUserException("HR admin accounts cannot have a manager");
            }
            user.setManager(null);
        } else {
            if (requestDto.getManagerId() == null) {
                if (requestDto.getUserRole() == UserRole.EMPLOYEE) {
                    throw new InvalidEmployeeManagerException("Employee must have a manager with role MANAGER");
                }
                throw new InvalidManagerSupervisorException("Manager must have a supervisor with role HR_ADMIN");
            }
            User manager = getById(requestDto.getManagerId());
            assertManagerAssignmentValid(null, requestDto.getUserRole(), manager);
            user.setManager(manager);
        }
        return user;
    }

    private void validateNewUserData(CreateUserRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new EmailAlreadyRegisteredException("user already exists with that email");
        }
        if (userRepository.existsByUsername(requestDto.getUsername())) {
            throw new UsernameAlreadyExists("username unavailable");
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
