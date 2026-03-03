package com.example.employeetimetracking.service;

import com.example.employeetimetracking.model.entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResult {
    private User user;
    private String token;
}
