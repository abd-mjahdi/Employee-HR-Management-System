package com.example.employeetimetracking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapCompanyRequestDto {

    @NotBlank
    @Size(max = 255)
    private String companyName;

    @NotBlank
    @Size(max = 63)
    private String slug;

    @NotBlank
    @Email
    @Size(max = 255)
    private String adminEmail;

    @NotBlank
    @Size(max = 50)
    private String adminFirstName;

    @NotBlank
    @Size(max = 50)
    private String adminLastName;

    /**
     * Optional. When null or blank, a temporary password is generated and returned once.
     */
    @Size(min = 6, max = 72)
    private String adminPassword;

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = (adminPassword == null || adminPassword.isBlank()) ? null : adminPassword;
    }
}
