package com.example.employeetimetracking.unit.service.timeentry;

import com.example.employeetimetracking.dto.request.CreateTimeEntryBreakDto;
import com.example.employeetimetracking.dto.request.CreateTimeEntryDto;
import com.example.employeetimetracking.dto.response.TimeEntryBreakDto;
import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.exception.InvalidTimeEntryException;
import com.example.employeetimetracking.mapper.TimeEntryMapper;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.TimeEntryBreak;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.CompanyStatus;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.TimeEntryBreakRepository;
import com.example.employeetimetracking.repository.TimeEntryRepository;
import com.example.employeetimetracking.service.LeaveRequestService;
import com.example.employeetimetracking.service.ProjectService;
import com.example.employeetimetracking.service.TimeEntryService;
import com.example.employeetimetracking.service.UserService;
import com.example.employeetimetracking.tenant.MembershipAccess;
import com.example.employeetimetracking.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TimeEntryServiceTest {
    @Mock
    TimeEntryRepository timeEntryRepository;

    @Mock
    TimeEntryMapper timeEntryMapper;

    @Mock
    UserService userService;

    @Mock
    ProjectService projectService;

    @Mock
    LeaveRequestService leaveRequestService;

    @Mock
    TimeEntryBreakRepository timeEntryBreakRepository;

    @Mock
    MembershipAccess membershipAccess;

    @Mock
    CompanyRepository companyRepository;

    @InjectMocks
    TimeEntryService timeEntryService;

    Company company;
    Department dept;
    User emp1;
    Project project;

    @BeforeEach
    public void setup() {
        TenantContext.set(new TenantContext.TenantInfo(1L, "acme", CompanyStatus.ACTIVE));
        company = new Company();
        company.setId(1L);
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);
        dept = new Department();
        dept.setId(1L);
        dept.setDepartmentName("Engineering");
        dept.setDepartmentCode("ENG");
        dept.setIsActive(true);

        emp1 = new User();
        emp1.setId(1L);
        emp1.setUsername("emp1");
        emp1.setEmail("emp1@example.com");
        emp1.setPasswordHash("$2a$10$fakehashfakehashfakehashfakehashfakehashfake");
        emp1.setFirstName("test");
        emp1.setLastName("test");
        emp1.setIsActive(true);

        project = new Project();
        project.setId(1L);
        project.setProjectName("Acme Portal");
        project.setProjectCode("ACM-001");
        project.setDescription("Internal rebuild");
        project.setIsActive(true);
        project.setCompany(company);
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    public void shouldCreatedTimeEntry() {
        when(userService.getById(1L)).thenReturn(emp1);
        when(projectService.getById(1L)).thenReturn(project);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(leaveRequestService.hasActiveLeaveRequestOnDate(any(User.class), any(LocalDate.class), anyList()))
                .thenReturn(false);
        when(timeEntryRepository.findByCompanyIdAndUserIdAndEntryDate(eq(1L), any(Long.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> {
            TimeEntry te = invocation.getArgument(0);
            te.setId(100L);
            return te;
        });
        when(timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(100L))
                .thenReturn(Collections.emptyList());
        when(timeEntryMapper.toDto(any(TimeEntry.class), anyList())).thenAnswer(invocation -> {
            TimeEntry te = invocation.getArgument(0);
            TimeEntryDto mapped = new TimeEntryDto();
            mapped.setId(te.getId());
            mapped.setTotalHours(te.getTotalHours());
            mapped.setBreaks(invocation.getArgument(1));
            return mapped;
        });

        CreateTimeEntryDto request = new CreateTimeEntryDto();
        request.setEntryDate(LocalDate.now());
        request.setClockInTime(LocalTime.of(9, 0));
        request.setClockOutTime(LocalTime.of(17, 0));
        request.setProjectId(1L);
        request.setDescription("test entry");

        TimeEntryDto te = timeEntryService.create(request, 1L);

        assertNotNull(te);
        assertEquals(new BigDecimal("8.00"), te.getTotalHours());
        ArgumentCaptor<TimeEntry> created = ArgumentCaptor.forClass(TimeEntry.class);
        verify(timeEntryRepository, times(1)).save(created.capture());
        assertEquals(1L, created.getValue().getCompany().getId());
    }

    @Test
    public void create_withSubmittedUnpaidBreaks_setsPayableHoursAndPersistsBreaks() {
        when(userService.getById(1L)).thenReturn(emp1);
        when(projectService.getById(1L)).thenReturn(project);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(leaveRequestService.hasActiveLeaveRequestOnDate(any(User.class), any(LocalDate.class), anyList()))
                .thenReturn(false);
        when(timeEntryRepository.findByCompanyIdAndUserIdAndEntryDate(eq(1L), any(Long.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> {
            TimeEntry te = invocation.getArgument(0);
            te.setId(100L);
            return te;
        });
        when(timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(100L)).thenAnswer(invocation -> {
            // After cascade save, breaks are on the entity; simulate DB return from captored save
            return List.of();
        });
        ArgumentCaptor<TimeEntry> saved = ArgumentCaptor.forClass(TimeEntry.class);
        when(timeEntryMapper.toDto(any(TimeEntry.class), anyList())).thenAnswer(invocation -> {
            TimeEntry te = invocation.getArgument(0);
            TimeEntryDto mapped = new TimeEntryDto();
            mapped.setId(te.getId());
            mapped.setTotalHours(te.getTotalHours());
            mapped.setBreaks(invocation.getArgument(1));
            return mapped;
        });

        CreateTimeEntryDto request = new CreateTimeEntryDto();
        request.setEntryDate(LocalDate.now());
        request.setClockInTime(LocalTime.of(9, 0));
        request.setClockOutTime(LocalTime.of(17, 0));
        request.setProjectId(1L);
        request.setDescription("day with lunch");
        request.setBreaks(List.of(
                new CreateTimeEntryBreakDto(LocalTime.of(12, 0), LocalTime.of(13, 0), true)
        ));

        TimeEntryDto result = timeEntryService.create(request, 1L);

        verify(timeEntryRepository).save(saved.capture());
        TimeEntry persisted = saved.getValue();
        assertEquals(1, persisted.getBreaks().size());
        assertEquals(LocalTime.of(12, 0), persisted.getBreaks().get(0).getBreakStart());
        assertEquals(new BigDecimal("7.00"), persisted.getTotalHours());
        assertEquals(new BigDecimal("7.00"), result.getTotalHours());
        assertEquals(1L, persisted.getCompany().getId());
    }

    @Test
    public void addUnpaidBreak_recalculatesPayableHours() {
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));
        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(1L)).thenReturn(emp1);
        when(timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(10L))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(breakOf(te, LocalTime.of(12, 0), LocalTime.of(13, 0), true)));
        when(timeEntryBreakRepository.save(any(TimeEntryBreak.class))).thenAnswer(invocation -> {
            TimeEntryBreak b = invocation.getArgument(0);
            b.setId(50L);
            return b;
        });

        CreateTimeEntryBreakDto dto = new CreateTimeEntryBreakDto(LocalTime.of(12, 0), LocalTime.of(13, 0), true);
        TimeEntryBreakDto result = timeEntryService.addBreak(10L, dto, 1L);

        assertEquals(60, result.getDurationMinutes());
        assertEquals(new BigDecimal("7.00"), te.getTotalHours());
    }

    @Test
    public void addPaidBreak_doesNotReducePayableHours() {
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));
        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(1L)).thenReturn(emp1);
        when(timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(10L))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(breakOf(te, LocalTime.of(12, 0), LocalTime.of(12, 30), false)));
        when(timeEntryBreakRepository.save(any(TimeEntryBreak.class))).thenAnswer(invocation -> {
            TimeEntryBreak b = invocation.getArgument(0);
            b.setId(51L);
            return b;
        });

        CreateTimeEntryBreakDto dto = new CreateTimeEntryBreakDto(LocalTime.of(12, 0), LocalTime.of(12, 30), false);
        timeEntryService.addBreak(10L, dto, 1L);

        assertEquals(new BigDecimal("8.00"), te.getTotalHours());
    }

    @Test
    public void deleteBreak_recalculatesPayableHours() {
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("7.00"));
        TimeEntryBreak unpaid = breakOf(te, LocalTime.of(12, 0), LocalTime.of(13, 0), true);
        unpaid.setId(50L);

        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(1L)).thenReturn(emp1);
        when(timeEntryBreakRepository.findById(50L)).thenReturn(Optional.of(unpaid));
        when(timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(10L))
                .thenReturn(Collections.emptyList());

        timeEntryService.deleteBreak(10L, 50L, 1L);

        verify(timeEntryBreakRepository).delete(unpaid);
        assertEquals(new BigDecimal("8.00"), te.getTotalHours());
    }

    @Test
    public void update_withSubmittedBreaks_replacesBreakSetAndRecalculates() {
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("7.00"));
        TimeEntryBreak oldBreak = breakOf(te, LocalTime.of(12, 0), LocalTime.of(13, 0), true);
        oldBreak.setId(50L);
        te.getBreaks().add(oldBreak);

        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(1L)).thenReturn(emp1);
        when(projectService.getById(1L)).thenReturn(project);
        when(timeEntryRepository.findByCompanyIdAndUserIdAndEntryDate(eq(1L), any(Long.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(10L))
                .thenReturn(List.of());
        when(timeEntryMapper.toDto(any(TimeEntry.class), anyList())).thenAnswer(invocation -> {
            TimeEntry saved = invocation.getArgument(0);
            TimeEntryDto dto = new TimeEntryDto();
            dto.setTotalHours(saved.getTotalHours());
            dto.setBreaks(invocation.getArgument(1));
            return dto;
        });

        CreateTimeEntryDto request = new CreateTimeEntryDto();
        request.setEntryDate(LocalDate.now().minusDays(1));
        request.setClockInTime(LocalTime.of(8, 0));
        request.setClockOutTime(LocalTime.of(17, 0));
        request.setProjectId(1L);
        request.setDescription("updated with new breaks");
        request.setBreaks(List.of(
                new CreateTimeEntryBreakDto(LocalTime.of(12, 0), LocalTime.of(12, 30), true)
        ));

        TimeEntryDto result = timeEntryService.update(10L, request, 1L);

        // 9h clock span - 0.5h unpaid break
        assertEquals(new BigDecimal("8.50"), result.getTotalHours());
        assertEquals(1, te.getBreaks().size());
        assertEquals(LocalTime.of(12, 30), te.getBreaks().get(0).getBreakEnd());
    }

    @Test
    public void approve_byDirectManager_succeeds() {
        User manager = managerUser(20L);
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));

        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(20L)).thenReturn(manager);
        when(membershipAccess.isDirectManagerOf(20L, 1L)).thenReturn(true);

        timeEntryService.approve(10L, 20L);

        assertEquals(Status.APPROVED, te.getStatus());
        assertEquals(manager, te.getApprovedBy());
        assertNull(te.getRejectionReason());
    }

    @Test
    public void approve_ownEntry_isRejected() {
        User manager = managerUser(20L);
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));
        te.setUser(manager);

        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(20L)).thenReturn(manager);

        assertThrows(InvalidTimeEntryException.class, () -> timeEntryService.approve(10L, 20L));
        assertEquals(Status.PENDING, te.getStatus());
    }

    @Test
    public void reject_setsRejectionReasonWithoutTouchingDescription() {
        User manager = managerUser(20L);
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));
        te.setDescription("worked on ACME");

        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(20L)).thenReturn(manager);
        when(membershipAccess.isDirectManagerOf(20L, 1L)).thenReturn(true);

        timeEntryService.reject(10L, 20L, "Hours look inflated");

        assertEquals(Status.DENIED, te.getStatus());
        assertEquals("Hours look inflated", te.getRejectionReason());
        assertEquals("worked on ACME", te.getDescription());
    }

    @Test
    public void deletePending_ownerCanCancelWithoutTimeLimit() {
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));
        te.setCreatedAt(LocalDateTime.now().minusDays(5));

        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(1L)).thenReturn(emp1);

        timeEntryService.deletePending(10L, 1L);

        assertEquals(Status.CANCELLED, te.getStatus());
    }

    @Test
    public void correctionFlow_unlockClearsApprovalAndAllowsPendingEdit() {
        User manager = managerUser(20L);
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));
        te.setStatus(Status.APPROVED);
        te.setApprovedBy(manager);
        te.setApprovedAt(LocalDateTime.now().minusDays(1));

        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(1L)).thenReturn(emp1);
        when(userService.getById(20L)).thenReturn(manager);
        when(membershipAccess.isDirectManagerOf(20L, 1L)).thenReturn(true);

        timeEntryService.requestCorrection(10L, 1L, "Forgot lunch break");
        assertEquals(Status.PENDING_CORRECTION, te.getStatus());
        assertEquals("Forgot lunch break", te.getCorrectionReason());

        timeEntryService.approveCorrectionUnlock(10L, 20L);
        assertEquals(Status.PENDING, te.getStatus());
        assertNull(te.getApprovedBy());
        assertNull(te.getApprovedAt());
    }

    @Test
    public void denyCorrection_restoresApproved() {
        User manager = managerUser(20L);
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));
        te.setStatus(Status.PENDING_CORRECTION);
        te.setApprovedBy(manager);
        te.setApprovedAt(LocalDateTime.now().minusDays(1));
        te.setCorrectionReason("Need to fix hours");

        when(timeEntryRepository.findByIdAndCompanyId(10L, 1L)).thenReturn(Optional.of(te));
        when(userService.getById(20L)).thenReturn(manager);
        when(membershipAccess.isDirectManagerOf(20L, 1L)).thenReturn(true);

        timeEntryService.denyCorrectionUnlock(10L, 20L);

        assertEquals(Status.APPROVED, te.getStatus());
        assertEquals(manager, te.getApprovedBy());
        assertEquals("Need to fix hours", te.getCorrectionReason());
    }

    @Test
    public void approve_missingInCurrentCompany_notFound() {
        when(timeEntryRepository.findByIdAndCompanyId(99L, 1L)).thenReturn(Optional.empty());
        when(userService.getById(20L)).thenReturn(managerUser(20L));

        assertThrows(InvalidTimeEntryException.class, () -> timeEntryService.approve(99L, 20L));
        verify(timeEntryRepository, never()).findById(99L);
    }

    @Test
    public void create_bindsCompanyFromContext() {
        when(userService.getById(1L)).thenReturn(emp1);
        when(projectService.getById(1L)).thenReturn(project);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(leaveRequestService.hasActiveLeaveRequestOnDate(any(User.class), any(LocalDate.class), anyList()))
                .thenReturn(false);
        when(timeEntryRepository.findByCompanyIdAndUserIdAndEntryDate(eq(1L), any(Long.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(timeEntryMapper.toDto(any(TimeEntry.class), anyList())).thenReturn(new TimeEntryDto());

        CreateTimeEntryDto request = new CreateTimeEntryDto();
        request.setEntryDate(LocalDate.now());
        request.setClockInTime(LocalTime.of(9, 0));
        request.setClockOutTime(LocalTime.of(17, 0));
        request.setProjectId(1L);
        request.setDescription("bind company");

        timeEntryService.create(request, 1L);

        ArgumentCaptor<TimeEntry> captor = ArgumentCaptor.forClass(TimeEntry.class);
        verify(timeEntryRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getCompany().getId());
        verify(timeEntryRepository, never()).findByUserIdAndEntryDate(any(Long.class), any(LocalDate.class));
    }

    @Test
    public void pendingApprovalsCount_usesCurrentCompanyOnly() {
        when(timeEntryRepository.countByUserManagerIdAndStatusForCompany(20L, Status.PENDING, 1L)).thenReturn(4);

        Integer count = timeEntryService.getPendingTimeApprovalsCount(20L);

        assertEquals(4, count);
        verify(timeEntryRepository, never()).countByUserManagerIdAndStatus(20L, Status.PENDING);
    }

    @Test
    public void userPendingCount_usesCurrentCompanyOnly() {
        when(timeEntryRepository.countByCompanyIdAndUserIdAndStatus(1L, 1L, Status.PENDING)).thenReturn(2);

        Integer count = timeEntryService.getUserPendingCount(1L);

        assertEquals(2, count);
        verify(timeEntryRepository, never()).countByUserIdAndStatus(1L, Status.PENDING);
    }

    @Test
    public void hrPendingQueue_scopesToCurrentCompany() {
        when(timeEntryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(Collections.emptyList());

        timeEntryService.getPendingApprovalQueue(2L, true);

        verify(timeEntryRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Sort.class));
        verify(timeEntryRepository, never()).findAll();
        verify(timeEntryRepository, never()).findByStatus(Status.PENDING);
    }

    private User managerUser(Long id) {
        User manager = new User();
        manager.setId(id);
        manager.setUsername("mgr");
        manager.setEmail("mgr@example.com");
        manager.setPasswordHash("hash");
        manager.setFirstName("Mgr");
        manager.setLastName("User");
        manager.setIsActive(true);
        return manager;
    }

    private TimeEntry pendingEntry(Long id, LocalTime in, LocalTime out, BigDecimal hours) {
        TimeEntry te = new TimeEntry();
        te.setId(id);
        te.setCompany(company);
        te.setUser(emp1);
        te.setEntryDate(LocalDate.now().minusDays(1));
        te.setClockInTime(in);
        te.setClockOutTime(out);
        te.setTotalHours(hours);
        te.setProject(project);
        te.setStatus(Status.PENDING);
        te.setCreatedAt(LocalDateTime.now());
        return te;
    }

    private TimeEntryBreak breakOf(TimeEntry te, LocalTime start, LocalTime end, boolean unpaid) {
        TimeEntryBreak b = new TimeEntryBreak();
        b.setTimeEntry(te);
        b.setBreakStart(start);
        b.setBreakEnd(end);
        b.setIsUnpaid(unpaid);
        return b;
    }
}
