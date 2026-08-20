package com.example.employeetimetracking.unit.service.user;

import com.example.employeetimetracking.dto.request.CreateUserRequestDto;
import com.example.employeetimetracking.dto.request.UserRequestDto;
import com.example.employeetimetracking.dto.response.UserCreatedResponse;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.exception.DepartmentNotFoundException;
import com.example.employeetimetracking.exception.EmailAlreadyRegisteredException;
import com.example.employeetimetracking.exception.InvalidEmployeeManagerException;
import com.example.employeetimetracking.exception.InvalidUserException;
import com.example.employeetimetracking.mapper.UserMapper;
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
import com.example.employeetimetracking.service.LeaveBalanceService;
import com.example.employeetimetracking.service.UserService;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    CompanyRepository companyRepository;
    @Mock
    CompanyMembershipRepository companyMembershipRepository;
    @Mock
    BCryptPasswordEncoder encoder;
    @Mock
    UserMapper userMapper;
    @Mock
    LeaveBalanceService leaveBalanceService;

    @InjectMocks
    UserService userService;

    Company company;
    Department dept;
    User hrAdmin;
    User manager;
    User employee;
    CompanyMembership hrMembership;
    CompanyMembership managerMembership;
    CompanyMembership employeeMembership;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);

        dept = new Department();
        dept.setId(1L);
        dept.setCompany(company);
        dept.setDepartmentName("Engineering");
        dept.setIsActive(true);

        hrAdmin = user(1L, "hr");
        manager = user(2L, "mgr");
        employee = user(3L, "emp");

        hrMembership = membership(1L, hrAdmin, UserRole.HR_ADMIN, null);
        managerMembership = membership(2L, manager, UserRole.MANAGER, hrMembership);
        employeeMembership = membership(3L, employee, UserRole.EMPLOYEE, managerMembership);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createEmployee_requiresManagerRole() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(departmentRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(dept));
        when(companyMembershipRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(hrMembership));

        CreateUserRequestDto dto = new CreateUserRequestDto(
                "newemp", "new@ex.com", "New", "Emp", UserRole.EMPLOYEE, 1L, 1L);

        assertThrows(InvalidEmployeeManagerException.class, () -> userService.createUser(dto));
    }

    @Test
    void createHrAdmin_rejectsManagerAssignment() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(departmentRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(dept));

        CreateUserRequestDto dto = new CreateUserRequestDto(
                "hr2", "hr2@ex.com", "Hr", "Two", UserRole.HR_ADMIN, 1L, 2L);

        assertThrows(InvalidUserException.class, () -> userService.createUser(dto));
    }

    @Test
    void createEmployee_withValidManager_succeeds() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(userRepository.findByEmail("new@ex.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("newemp")).thenReturn(false);
        when(departmentRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(dept));
        when(companyMembershipRepository.findByIdAndCompanyId(2L, 1L)).thenReturn(Optional.of(managerMembership));
        when(encoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(companyMembershipRepository.save(any(CompanyMembership.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(any(User.class), any())).thenReturn(new UserResponseDto());

        CreateUserRequestDto dto = new CreateUserRequestDto(
                "newemp", "new@ex.com", "New", "Emp", UserRole.EMPLOYEE, 1L, 2L);

        UserCreatedResponse response = userService.createUser(dto);

        assertNotNull(response);
        assertNotNull(response.getTemporaryPass());
        verify(leaveBalanceService).initializeLeaveBalances(any(User.class));

        ArgumentCaptor<CompanyMembership> membershipCaptor = ArgumentCaptor.forClass(CompanyMembership.class);
        verify(companyMembershipRepository).save(membershipCaptor.capture());
        CompanyMembership created = membershipCaptor.getValue();
        assertEquals(UserRole.EMPLOYEE, created.getRole());
        assertEquals(MembershipStatus.ACTIVE, created.getStatus());
        assertEquals(1L, created.getCompany().getId());
        assertEquals(2L, created.getManagerMembership().getId());
    }

    @Test
    void createUser_existingEmailInOtherCompany_doesNotResetPassword() {
        User existing = user(40L, "pat");
        existing.setEmail("pat@ex.com");
        existing.setPasswordHash("original-hash");

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(userRepository.findByEmail("pat@ex.com")).thenReturn(Optional.of(existing));
        when(departmentRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(dept));
        when(companyMembershipRepository.findByIdAndCompanyId(2L, 1L)).thenReturn(Optional.of(managerMembership));
        when(companyMembershipRepository.findByUserIdAndCompanyId(40L, 1L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(companyMembershipRepository.save(any(CompanyMembership.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(any(User.class), any())).thenReturn(new UserResponseDto());

        CreateUserRequestDto dto = new CreateUserRequestDto(
                "ignored", "pat@ex.com", "Pat", "Lee", UserRole.EMPLOYEE, 1L, 2L);

        UserCreatedResponse response = userService.createUser(dto);

        assertNull(response.getTemporaryPass());
        assertEquals("original-hash", existing.getPasswordHash());
        verify(encoder, never()).encode(anyString());
        verify(leaveBalanceService).initializeLeaveBalances(existing);
    }

    @Test
    void createUser_alreadyMemberOfTenant_isConflict() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(userRepository.findByEmail("emp@ex.com")).thenReturn(Optional.of(employee));
        when(departmentRepository.findByIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(dept));
        when(companyMembershipRepository.findByIdAndCompanyId(2L, 1L)).thenReturn(Optional.of(managerMembership));
        when(companyMembershipRepository.findByUserIdAndCompanyId(3L, 1L)).thenReturn(Optional.of(membership(3L, employee, UserRole.EMPLOYEE, managerMembership)));

        CreateUserRequestDto dto = new CreateUserRequestDto(
                "emp", "emp@ex.com", "Emp", "User", UserRole.EMPLOYEE, 1L, 2L);

        assertThrows(EmailAlreadyRegisteredException.class, () -> userService.createUser(dto));
        verify(companyMembershipRepository, never()).save(any());
    }

    @Test
    void createUser_departmentOutsideTenant_notFound() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(departmentRepository.findByIdAndCompanyId(9L, 1L)).thenReturn(Optional.empty());

        CreateUserRequestDto dto = new CreateUserRequestDto(
                "newemp", "new@ex.com", "New", "Emp", UserRole.EMPLOYEE, 9L, 2L);

        assertThrows(DepartmentNotFoundException.class, () -> userService.createUser(dto));
    }

    @Test
    void update_roleToManager_revalidatesExistingSupervisor() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(companyMembershipRepository.findByUserIdAndCompanyId(3L, 1L)).thenReturn(Optional.of(employeeMembership));
        when(companyMembershipRepository.findByIdAndCompanyId(2L, 1L)).thenReturn(Optional.of(managerMembership));
        UserRequestDto dto = new UserRequestDto();
        dto.setUserRole(UserRole.MANAGER);

        assertThrows(Exception.class, () -> userService.updateUser(3L, dto));
    }

    @Test
    void update_selfAsManager_rejected() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(companyMembershipRepository.findByUserIdAndCompanyId(3L, 1L)).thenReturn(Optional.of(employeeMembership));
        when(companyMembershipRepository.findByIdAndCompanyId(3L, 1L)).thenReturn(Optional.of(employeeMembership));
        UserRequestDto dto = new UserRequestDto();
        dto.setManagerId(3L);

        assertThrows(InvalidUserException.class, () -> userService.updateUser(3L, dto));
    }

    @Test
    void deactivate_self_rejected() {
        assertThrows(InvalidUserException.class, () -> userService.deactivateUserById(1L, 1L));
    }

    @Test
    void deactivate_lastHrAdmin_rejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(hrAdmin));
        when(companyMembershipRepository.findByUserIdAndCompanyId(1L, 1L)).thenReturn(Optional.of(hrMembership));
        when(companyMembershipRepository.countByCompanyIdAndRoleAndStatus(1L, UserRole.HR_ADMIN, MembershipStatus.ACTIVE))
                .thenReturn(1L);

        assertThrows(InvalidUserException.class, () -> userService.deactivateUserById(1L, 2L));
    }

    @Test
    void deactivate_withActiveReports_rejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(companyMembershipRepository.findByUserIdAndCompanyId(2L, 1L)).thenReturn(Optional.of(managerMembership));
        when(companyMembershipRepository.findByCompanyIdAndManagerMembershipIdAndStatus(1L, 2L, MembershipStatus.ACTIVE))
                .thenReturn(List.of(employeeMembership));

        assertThrows(InvalidUserException.class, () -> userService.deactivateUserById(2L, 1L));
    }

    @Test
    void deactivate_okWhenNoReports() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(companyMembershipRepository.findByUserIdAndCompanyId(3L, 1L)).thenReturn(Optional.of(employeeMembership));
        when(companyMembershipRepository.findByCompanyIdAndManagerMembershipIdAndStatus(1L, 3L, MembershipStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        userService.deactivateUserById(3L, 1L);

        assertFalse(employee.getIsActive());
        assertEquals(MembershipStatus.DEACTIVATED, employeeMembership.getStatus());
    }

    @Test
    void getTeamMembers_returnsOnlyActiveReports() {
        when(companyMembershipRepository.findByUserIdAndCompanyId(2L, 1L)).thenReturn(Optional.of(managerMembership));
        when(companyMembershipRepository.findByCompanyIdAndManagerMembershipIdAndStatus(1L, 2L, MembershipStatus.ACTIVE))
                .thenReturn(List.of(employeeMembership));
        when(userMapper.toDto(employee, employeeMembership)).thenReturn(new UserResponseDto());

        List<UserResponseDto> team = userService.getTeamMembers(2L);

        assertEquals(1, team.size());
        verify(companyMembershipRepository).findByCompanyIdAndManagerMembershipIdAndStatus(
                1L, 2L, MembershipStatus.ACTIVE);
        verify(companyMembershipRepository, never()).findByManagerMembershipIdAndStatus(2L, MembershipStatus.ACTIVE);
    }

    @Test
    void getAll_listsCurrentCompanyMembershipsOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        when(companyMembershipRepository.findByCompanyId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(employeeMembership, managerMembership)));
        when(userMapper.toDto(employee, employeeMembership)).thenReturn(new UserResponseDto());
        when(userMapper.toDto(manager, managerMembership)).thenReturn(new UserResponseDto());

        Page<UserResponseDto> result = userService.getAll(pageable);

        assertEquals(2, result.getContent().size());
        verify(companyMembershipRepository).findByCompanyId(1L, pageable);
        verify(userRepository, never()).findAll(pageable);
        verify(userRepository, never()).findAll();
    }

    @Test
    void searchUsers_queriesMembershipsInCurrentCompany() {
        when(companyMembershipRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(employeeMembership));
        when(userMapper.toDto(employee, employeeMembership)).thenReturn(new UserResponseDto());

        List<UserResponseDto> result = userService.searchUsers(1L, UserRole.EMPLOYEE, true, "emp");

        assertEquals(1, result.size());
        verify(companyMembershipRepository).findAll(any(Specification.class));
        verify(userRepository, never()).findAll(any(Specification.class));
        verify(userRepository, never()).findAll();
    }

    private CompanyMembership membership(Long id, User user, UserRole role, CompanyMembership manager) {
        CompanyMembership membership = new CompanyMembership();
        membership.setId(id);
        membership.setUser(user);
        membership.setCompany(company);
        membership.setRole(role);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setDepartment(dept);
        membership.setManagerMembership(manager);
        return membership;
    }

    private User user(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@ex.com");
        u.setPasswordHash("hash");
        u.setFirstName(username);
        u.setLastName("User");
        u.setIsActive(true);
        return u;
    }
}
