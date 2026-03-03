package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.LoginRequestDto;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public LoginService(UserRepository userRepository, JwtUtil jwtUtil, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDto login(LoginRequestDto requestDto) {
        User user = userRepository.findByEmail(requestDto.getEmail());

        if (user == null || !passwordEncoder.matches(requestDto.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account deactivated");
        }

        String token = jwtUtil.generateJwtToken(user.getEmail(), user.getId(), user.getUserRole());

        return new LoginResponseDto(token, user.getEmail(), user.getUserRole());

    }
}