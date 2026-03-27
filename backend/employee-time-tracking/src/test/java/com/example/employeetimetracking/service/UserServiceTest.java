package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateUserRequestDto;
import com.example.employeetimetracking.dto.response.UserCreatedResponse;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.exception.EmailAlreadyRegisteredException;
import com.example.employeetimetracking.exception.UsernameAlreadyExists;
import com.example.employeetimetracking.mapper.UserMapper;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DepartmentService departmentService; // was missing entirely

    @Mock
    private BCryptPasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_success() {

        // manager that already exists in the system
        User manager = new User();
        manager.setId(1L);
        manager.setUserRole(UserRole.MANAGER); // whatever role passes validateManagerAssignment

        Department department = new Department();
        department.setId(10L);

        CreateUserRequestDto request = new CreateUserRequestDto();
        request.setEmail("test@test.com");
        request.setUsername("testuser");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setUserRole(UserRole.EMPLOYEE);
        request.setManagerId(1L);
        request.setDepartmentId(10L);

        User savedUser = new User();
        UserResponseDto userDto = new UserResponseDto();

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);

        // mock the manager lookup inside getById
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));

        // mock the department lookup
        when(departmentService.getById(10L)).thenReturn(department);
        when(encoder.encode(any())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(userDto);

        UserCreatedResponse response = userService.createUser(request);

        verify(userRepository, times(1)).save(any(User.class));
        verify(leaveBalanceService, times(1)).initializeLeaveBalances(savedUser);
        assertNotNull(response);
        assertEquals(userDto, response.getUserResponseDto());
        assertNotNull(response.getTemporaryPass());
        assertFalse(response.getTemporaryPass().isBlank());
    }

    @Test
    void createUser_failure_emailAlreadyExists() {

        CreateUserRequestDto request = new CreateUserRequestDto();
        request.setEmail("test@test.com");
        request.setUsername("testuser");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setUserRole(UserRole.EMPLOYEE);
        request.setManagerId(1L);
        request.setDepartmentId(10L);

        // email is taken
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // assert the exception is thrown
        assertThrows(EmailAlreadyRegisteredException.class,
                () -> userService.createUser(request));

        // save should never be reached
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_failure_usernameAlreadyExists() {

        CreateUserRequestDto request = new CreateUserRequestDto();
        request.setEmail("test@test.com");
        request.setUsername("testuser");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setUserRole(UserRole.EMPLOYEE);
        request.setManagerId(1L);
        request.setDepartmentId(10L);

        // email is free but username is taken
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(UsernameAlreadyExists.class,
                () -> userService.createUser(request));

        verify(userRepository, never()).save(any(User.class));
    }
}