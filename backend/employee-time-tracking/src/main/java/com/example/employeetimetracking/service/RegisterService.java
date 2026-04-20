package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.RegisterRequestDto;
import com.example.employeetimetracking.dto.response.RegisterResponseDto;
import com.example.employeetimetracking.exception.DepartmentNotFoundException;
import com.example.employeetimetracking.exception.EmailAlreadyRegisteredException;
import com.example.employeetimetracking.exception.InvalidUserRoleException;
import com.example.employeetimetracking.exception.WeakPasswordException;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterService {
    private final DepartmentService departmentService;
    private final BCryptPasswordEncoder encoder;
    private final UserService userService;
    private final LeaveBalanceService leaveBalanceService;
    @Autowired
    public RegisterService(UserService userService,
                           BCryptPasswordEncoder encoder,
                           DepartmentService departmentService,
                           LeaveBalanceService leaveBalanceService) {
        this.userService = userService;
        this.encoder = encoder;
        this.departmentService = departmentService;
        this.leaveBalanceService = leaveBalanceService;
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto requestDto) {
        String email = requestDto.getEmail();

        Department dep = departmentService.getByDepartmentCode(requestDto.getDepartmentCode());

        if (userService.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        User manager = requestDto.getManagerId()==null ? null : userService.getById(requestDto.getManagerId());

        UserRole userRole;
        try {
            userRole = UserRole.valueOf(requestDto.getUserRole());
        } catch (IllegalArgumentException e) {
            throw new InvalidUserRoleException("Invalid user role");
        }

        String password = requestDto.getPassword();
        if (password.length() < 6) {
            throw new WeakPasswordException("Password must be at least 6 characters");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());
        user.setUsername(requestDto.getUsername());
        user.setManager(manager);
        user.setDepartment(dep);
        user.setUserRole(userRole);

        leaveBalanceService.initializeLeaveBalances(userService.save(user));


        return new RegisterResponseDto(email, userRole);
    }
}
