package com.example.employeetimetracking.unit.service.user;

import com.example.employeetimetracking.dto.request.CreateUserRequestDto;
import com.example.employeetimetracking.dto.request.UserRequestDto;
import com.example.employeetimetracking.dto.response.UserCreatedResponse;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.exception.InvalidEmployeeManagerException;
import com.example.employeetimetracking.exception.InvalidUserException;
import com.example.employeetimetracking.mapper.UserMapper;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.service.DepartmentService;
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    DepartmentService departmentService;
    @Mock
    BCryptPasswordEncoder encoder;
    @Mock
    UserMapper userMapper;
    @Mock
    LeaveBalanceService leaveBalanceService;

    @InjectMocks
    UserService userService;

    Department dept;
    User hrAdmin;
    User manager;
    User employee;

    @BeforeEach
    void setUp() {
        dept = new Department();
        dept.setId(1L);
        dept.setDepartmentName("Engineering");
        dept.setIsActive(true);

        hrAdmin = user(1L, "hr", UserRole.HR_ADMIN, null);
        manager = user(2L, "mgr", UserRole.MANAGER, hrAdmin);
        employee = user(3L, "emp", UserRole.EMPLOYEE, manager);
    }

    @Test
    void createEmployee_requiresManagerRole() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(hrAdmin)); // wrong: HR as employee manager

        CreateUserRequestDto dto = new CreateUserRequestDto(
                "newemp", "new@ex.com", "New", "Emp", UserRole.EMPLOYEE, 1L, 1L);

        assertThrows(InvalidEmployeeManagerException.class, () -> userService.createUser(dto));
    }

    @Test
    void createHrAdmin_rejectsManagerAssignment() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);

        CreateUserRequestDto dto = new CreateUserRequestDto(
                "hr2", "hr2@ex.com", "Hr", "Two", UserRole.HR_ADMIN, 1L, 2L);

        assertThrows(InvalidUserException.class, () -> userService.createUser(dto));
    }

    @Test
    void createEmployee_withValidManager_succeeds() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(departmentService.getById(1L)).thenReturn(dept);
        when(encoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(userMapper.toDto(any(User.class))).thenReturn(new UserResponseDto());

        CreateUserRequestDto dto = new CreateUserRequestDto(
                "newemp", "new@ex.com", "New", "Emp", UserRole.EMPLOYEE, 1L, 2L);

        UserCreatedResponse response = userService.createUser(dto);

        assertNotNull(response);
        assertNotNull(response.getTemporaryPass());
        verify(leaveBalanceService).initializeLeaveBalances(any(User.class));
    }

    @Test
    void update_roleToManager_revalidatesExistingSupervisor() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(employee));
        // employee currently reports to manager; promoting to MANAGER requires HR supervisor
        UserRequestDto dto = new UserRequestDto();
        dto.setUserRole(UserRole.MANAGER);

        assertThrows(Exception.class, () -> userService.updateUser(3L, dto));
    }

    @Test
    void update_selfAsManager_rejected() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(employee));
        UserRequestDto dto = new UserRequestDto();
        dto.setManagerId(3L);

        assertThrows(InvalidUserException.class, () -> userService.updateUser(3L, dto));
    }

    @Test
    void deactivate_self_rejected() {
        assertThrows(InvalidUserException.class, () -> userService.deactivateUserById(1L, 1L));
    }

    @Test
    void deactivate_lastHrAdmin_rejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(hrAdmin));
        when(userRepository.countByUserRoleAndIsActive(UserRole.HR_ADMIN, true)).thenReturn(1L);

        assertThrows(InvalidUserException.class, () -> userService.deactivateUserById(1L, 2L));
    }

    @Test
    void deactivate_withActiveReports_rejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(userRepository.findByManagerIdAndIsActive(2L, true)).thenReturn(List.of(employee));

        assertThrows(InvalidUserException.class, () -> userService.deactivateUserById(2L, 1L));
    }

    @Test
    void deactivate_okWhenNoReports() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(userRepository.findByManagerIdAndIsActive(3L, true)).thenReturn(Collections.emptyList());

        userService.deactivateUserById(3L, 1L);

        assertFalse(employee.getIsActive());
    }

    @Test
    void getTeamMembers_returnsOnlyActiveReports() {
        when(userRepository.findByManagerIdAndIsActive(2L, true)).thenReturn(List.of(employee));
        when(userMapper.toDto(employee)).thenReturn(new UserResponseDto());

        List<UserResponseDto> team = userService.getTeamMembers(2L);

        assertEquals(1, team.size());
        verify(userRepository).findByManagerIdAndIsActive(2L, true);
    }

    private User user(Long id, String username, UserRole role, User manager) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@ex.com");
        u.setPasswordHash("hash");
        u.setFirstName(username);
        u.setLastName("User");
        u.setUserRole(role);
        u.setDepartment(dept);
        u.setManager(manager);
        u.setIsActive(true);
        return u;
    }
}
