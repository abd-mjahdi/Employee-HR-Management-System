package com.example.employeetimetracking.unit.service.bootstrap;

import com.example.employeetimetracking.config.BootstrapProperties;
import com.example.employeetimetracking.config.TenantProperties;
import com.example.employeetimetracking.dto.request.BootstrapCompanyRequestDto;
import com.example.employeetimetracking.dto.response.BootstrapCompanyResponseDto;
import com.example.employeetimetracking.exception.BootstrapAlreadyCompletedException;
import com.example.employeetimetracking.exception.BootstrapDisabledException;
import com.example.employeetimetracking.exception.EmailAlreadyRegisteredException;
import com.example.employeetimetracking.exception.InvalidBootstrapKeyException;
import com.example.employeetimetracking.exception.InvalidSlugException;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.CompanyMembership;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.MembershipStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.CompanyMembershipRepository;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.service.BootstrapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapServiceTest {

    private static final String KEY = "test-bootstrap-secret";

    @Mock
    CompanyRepository companyRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    CompanyMembershipRepository companyMembershipRepository;
    @Mock
    BCryptPasswordEncoder passwordEncoder;

    BootstrapProperties bootstrapProperties;
    TenantProperties tenantProperties;
    BootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        bootstrapProperties = new BootstrapProperties();
        bootstrapProperties.setKey(KEY);
        tenantProperties = new TenantProperties();
        tenantProperties.setBaseDomain("myhr.com");
        tenantProperties.setReservedSlugs(List.of("www", "api", "app", "admin", "mail", "localhost"));
        bootstrapService = new BootstrapService(
                bootstrapProperties,
                tenantProperties,
                companyRepository,
                departmentRepository,
                userRepository,
                companyMembershipRepository,
                passwordEncoder
        );
    }

    @Test
    void bootstrap_createsActiveCompanyDefaultDepartmentAndHrAdmin() {
        when(companyRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("hr@northwind.com")).thenReturn(false);
        when(userRepository.existsByUsername("hr")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        stubSaves();

        BootstrapCompanyResponseDto response = bootstrapService.bootstrap(KEY, request("Northwind", "Northwind", "secret1"));

        assertEquals("Northwind", response.getCompanyName());
        assertEquals("northwind", response.getSlug());
        assertEquals("hr@northwind.com", response.getAdminEmail());
        assertNull(response.getTemporaryPassword());
        assertEquals("northwind.myhr.com", response.getLoginHost());

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        assertEquals(CompanyStatus.ACTIVE, companyCaptor.getValue().getStatus());
        assertEquals("northwind", companyCaptor.getValue().getSlug());

        ArgumentCaptor<Department> deptCaptor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(deptCaptor.capture());
        assertEquals("General", deptCaptor.getValue().getDepartmentName());
        assertEquals("GEN", deptCaptor.getValue().getDepartmentCode());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertEquals("hashed", admin.getPasswordHash());
        assertTrue(admin.getIsActive());

        ArgumentCaptor<CompanyMembership> membershipCaptor = ArgumentCaptor.forClass(CompanyMembership.class);
        verify(companyMembershipRepository).save(membershipCaptor.capture());
        CompanyMembership membership = membershipCaptor.getValue();
        assertEquals(UserRole.HR_ADMIN, membership.getRole());
        assertEquals(MembershipStatus.ACTIVE, membership.getStatus());
        assertEquals("GEN", membership.getDepartment().getDepartmentCode());
        assertEquals(companyCaptor.getValue(), membership.getCompany());
    }

    @Test
    void bootstrap_generatesTemporaryPasswordWhenOmitted() {
        when(companyRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        stubSaves();

        BootstrapCompanyRequestDto request = request("Acme", "acme", null);
        BootstrapCompanyResponseDto response = bootstrapService.bootstrap(KEY, request);

        assertNotNull(response.getTemporaryPassword());
        assertTrue(response.getTemporaryPassword().length() >= 8);
        verify(passwordEncoder).encode(response.getTemporaryPassword());
    }

    @Test
    void bootstrap_blankKeyConfig_isDisabled() {
        bootstrapProperties.setKey("");
        assertThrows(BootstrapDisabledException.class,
                () -> bootstrapService.bootstrap("anything", request("Acme", "acme", "secret1")));
        verify(companyRepository, never()).count();
    }

    @Test
    void bootstrap_wrongKey_isUnauthorized() {
        assertThrows(InvalidBootstrapKeyException.class,
                () -> bootstrapService.bootstrap("wrong", request("Acme", "acme", "secret1")));
        verify(companyRepository, never()).count();
    }

    @Test
    void bootstrap_missingKey_isUnauthorized() {
        assertThrows(InvalidBootstrapKeyException.class,
                () -> bootstrapService.bootstrap(null, request("Acme", "acme", "secret1")));
    }

    @Test
    void bootstrap_secondRun_isRejected() {
        when(companyRepository.count()).thenReturn(2L);
        assertThrows(BootstrapAlreadyCompletedException.class,
                () -> bootstrapService.bootstrap(KEY, request("Globex", "globex", "secret1")));
        verify(companyRepository, never()).save(any());
    }

    @Test
    void bootstrap_normalizesAdminEmail() {
        when(companyRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("hr@northwind.com")).thenReturn(false);
        when(userRepository.existsByUsername("hr")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        stubSaves();

        BootstrapCompanyRequestDto request = request("Northwind", "northwind", "secret1");
        request.setAdminEmail("HR@Northwind.com");

        BootstrapCompanyResponseDto response = bootstrapService.bootstrap(KEY, request);

        assertEquals("hr@northwind.com", response.getAdminEmail());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("hr@northwind.com", userCaptor.getValue().getEmail());
    }

    @Test
    void bootstrap_existingEmail_fails() {
        when(companyRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("hr@northwind.com")).thenReturn(true);
        assertThrows(EmailAlreadyRegisteredException.class,
                () -> bootstrapService.bootstrap(KEY, request("Northwind", "northwind", "secret1")));
    }

    @Test
    void bootstrap_reservedSlug_isRejected() {
        assertThrows(InvalidSlugException.class,
                () -> bootstrapService.bootstrap(KEY, request("Admin Co", "admin", "secret1")));
        verify(companyRepository, never()).count();
    }

    @Test
    void bootstrap_invalidSlugFormat_isRejected() {
        assertThrows(InvalidSlugException.class,
                () -> bootstrapService.bootstrap(KEY, request("Bad", "Not A Slug", "secret1")));
    }

    private void stubSaves() {
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company company = inv.getArgument(0);
            company.setId(1L);
            return company;
        });
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department department = inv.getArgument(0);
            department.setId(1L);
            return department;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(companyMembershipRepository.save(any(CompanyMembership.class))).thenAnswer(inv -> {
            CompanyMembership membership = inv.getArgument(0);
            membership.setId(1L);
            return membership;
        });
    }

    private static BootstrapCompanyRequestDto request(String name, String slug, String password) {
        BootstrapCompanyRequestDto dto = new BootstrapCompanyRequestDto();
        dto.setCompanyName(name);
        dto.setSlug(slug);
        dto.setAdminEmail("hr@northwind.com");
        dto.setAdminFirstName("Pat");
        dto.setAdminLastName("Lee");
        dto.setAdminPassword(password);
        return dto;
    }
}
