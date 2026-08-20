package com.example.employeetimetracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapCompanyResponseDto {
    private String companyName;
    private String slug;
    private String adminEmail;
    private String temporaryPassword;
    private String loginHost;
}
