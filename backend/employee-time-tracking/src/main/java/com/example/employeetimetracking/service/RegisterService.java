package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.RegisterRequestDto;
import com.example.employeetimetracking.dto.response.RegisterResponseDto;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterService {
    private final DepartmentRepository departmentRepository;
    private final BCryptPasswordEncoder encoder;
    private final UserRepository userRepository;
    @Autowired
    public RegisterService(UserRepository userRepository ,BCryptPasswordEncoder encoder ,DepartmentRepository departmentRepository){
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.departmentRepository = departmentRepository;
    }
    public RegisterResponseDto register(RegisterRequestDto requestDto) {
        String email = requestDto.getEmail();


        Department dep = departmentRepository.findByDepartmentCode(requestDto.getDepartmentCode());
        if (dep == null) {
            throw new IllegalArgumentException("Department doesn't exist");
        }


        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }


        User manager = userRepository.findById(requestDto.getManagerId())
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));


        UserRole userRole;
        try {
            userRole = UserRole.valueOf(requestDto.getUserRole());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user role");
        }


        String password = requestDto.getPassword();
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
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

        userRepository.save(user);

        return new RegisterResponseDto(email, userRole);
    }
}
