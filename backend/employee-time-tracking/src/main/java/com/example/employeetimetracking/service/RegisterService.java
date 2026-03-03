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
    public RegisterResponseDto register(RegisterRequestDto requestDto){
        String email = requestDto.getEmail();
        if(userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("Email already exists");
        };


        User user = new User();
        user.setEmail(email);
        String passwordHash = encoder.encode(requestDto.getPassword());
        user.setPasswordHash(passwordHash);
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());

        UserRole userRole = UserRole.valueOf(requestDto.getUserRole());

        Department dep = departmentRepository.findByDepartmentCode(requestDto.getDepartmentCode());
        if(dep==null){
            throw new IllegalArgumentException("Department doesn't exist");
        }
        user.setDepartment(dep);
        userRepository.save(user);
        return new RegisterResponseDto(requestDto.getEmail(), userRole);


    }
}
