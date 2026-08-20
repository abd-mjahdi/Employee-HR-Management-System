package com.example.employeetimetracking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LoginRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @ToString.Exclude
    private String password;
}
