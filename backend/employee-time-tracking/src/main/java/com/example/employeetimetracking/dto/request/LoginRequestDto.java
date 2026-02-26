package com.example.employeetimetracking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 10 , max = 20)
    private String password;
}
