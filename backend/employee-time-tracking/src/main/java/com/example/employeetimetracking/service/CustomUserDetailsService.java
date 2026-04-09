package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // treat email as username , too lazy to change to username
    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return new CustomUserDetails(user);
    }
}