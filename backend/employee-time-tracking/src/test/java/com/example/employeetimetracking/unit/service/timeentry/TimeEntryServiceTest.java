package com.example.employeetimetracking.unit.service.timeentry;

import com.example.employeetimetracking.dto.request.CreateTimeEntryBreakDto;
import com.example.employeetimetracking.dto.request.CreateTimeEntryDto;
import com.example.employeetimetracking.dto.response.TimeEntryBreakDto;
import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.mapper.TimeEntryMapper;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.TimeEntryBreak;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.TimeEntryBreakRepository;
import com.example.employeetimetracking.repository.TimeEntryRepository;
import com.example.employeetimetracking.service.LeaveRequestService;
import com.example.employeetimetracking.service.ProjectService;
import com.example.employeetimetracking.service.TimeEntryService;
import com.example.employeetimetracking.service.UserService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

    @InjectMocks
    TimeEntryService timeEntryService;

    Department dept;
    User emp1;
    Project project;

    @BeforeEach
    public void setup() {
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
        emp1.setUserRole(UserRole.EMPLOYEE);
        emp1.setDepartment(dept);
        emp1.setManager(null);
        emp1.setIsActive(true);

        project = new Project();
        project.setId(1L);
        project.setProjectName("Acme Portal");
        project.setProjectCode("ACM-001");
        project.setDescription("Internal rebuild");
        project.setIsActive(true);
    }

    @Test
    public void shouldCreatedTimeEntry() {
        when(userService.getById(1L)).thenReturn(emp1);
        when(projectService.getById(1L)).thenReturn(project);
        when(leaveRequestService.hasActiveLeaveRequestOnDate(any(User.class), any(LocalDate.class), anyList()))
                .thenReturn(false);
        when(timeEntryRepository.findByUserIdAndEntryDate(any(Long.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TimeEntryDto mapped = new TimeEntryDto();
        mapped.setId(100L);
        when(timeEntryMapper.toDto(any(TimeEntry.class))).thenReturn(mapped);

        CreateTimeEntryDto request = new CreateTimeEntryDto();
        request.setEntryDate(LocalDate.now());
        request.setClockInTime(LocalTime.of(9, 0));
        request.setClockOutTime(LocalTime.of(17, 0));
        request.setProjectId(1L);
        request.setDescription("test entry");

        TimeEntryDto te = timeEntryService.create(request, 1L);

        assertNotNull(te);
        ArgumentCaptor<TimeEntry> saved = ArgumentCaptor.forClass(TimeEntry.class);
        verify(timeEntryRepository, times(1)).save(saved.capture());
        assertEquals(new BigDecimal("8.00"), saved.getValue().getTotalHours());
        verify(timeEntryMapper).toDto(any(TimeEntry.class));
    }

    @Test
    public void addUnpaidBreak_recalculatesPayableHours() {
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));
        when(timeEntryRepository.findById(10L)).thenReturn(Optional.of(te));
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
        TimeEntryBreakDto result = timeEntryService.addBreak(10L, dto, 1L, false);

        assertEquals(60, result.getDurationMinutes());
        assertEquals(new BigDecimal("7.00"), te.getTotalHours());
    }

    @Test
    public void addPaidBreak_doesNotReducePayableHours() {
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("8.00"));
        when(timeEntryRepository.findById(10L)).thenReturn(Optional.of(te));
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
        timeEntryService.addBreak(10L, dto, 1L, false);

        assertEquals(new BigDecimal("8.00"), te.getTotalHours());
    }

    @Test
    public void deleteBreak_recalculatesPayableHours() {
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("7.00"));
        TimeEntryBreak unpaid = breakOf(te, LocalTime.of(12, 0), LocalTime.of(13, 0), true);
        unpaid.setId(50L);

        when(timeEntryRepository.findById(10L)).thenReturn(Optional.of(te));
        when(userService.getById(1L)).thenReturn(emp1);
        when(timeEntryBreakRepository.findById(50L)).thenReturn(Optional.of(unpaid));
        when(timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(10L))
                .thenReturn(Collections.emptyList());

        timeEntryService.deleteBreak(10L, 50L, 1L, false);

        verify(timeEntryBreakRepository).delete(unpaid);
        assertEquals(new BigDecimal("8.00"), te.getTotalHours());
    }

    @Test
    public void update_recalculatesPayableHoursWithExistingUnpaidBreaks() {
        TimeEntry te = pendingEntry(10L, LocalTime.of(9, 0), LocalTime.of(17, 0), new BigDecimal("7.00"));
        TimeEntryBreak unpaid = breakOf(te, LocalTime.of(12, 0), LocalTime.of(13, 0), true);
        unpaid.setId(50L);

        when(timeEntryRepository.findById(10L)).thenReturn(Optional.of(te));
        when(userService.getById(1L)).thenReturn(emp1);
        when(projectService.getById(1L)).thenReturn(project);
        when(timeEntryBreakRepository.findByTimeEntryIdOrderByBreakStartAsc(10L))
                .thenReturn(List.of(unpaid));
        when(timeEntryRepository.findByUserIdAndEntryDate(any(Long.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(timeEntryMapper.toDto(any(TimeEntry.class))).thenAnswer(invocation -> {
            TimeEntry saved = invocation.getArgument(0);
            TimeEntryDto dto = new TimeEntryDto();
            dto.setTotalHours(saved.getTotalHours());
            return dto;
        });

        CreateTimeEntryDto request = new CreateTimeEntryDto();
        request.setEntryDate(LocalDate.now().minusDays(1));
        request.setClockInTime(LocalTime.of(8, 0));
        request.setClockOutTime(LocalTime.of(17, 0));
        request.setProjectId(1L);
        request.setDescription("updated");

        TimeEntryDto result = timeEntryService.update(10L, request, 1L, false);

        // 9h clock span - 1h unpaid break
        assertEquals(new BigDecimal("8.00"), result.getTotalHours());
        assertEquals(new BigDecimal("8.00"), te.getTotalHours());
    }

    private TimeEntry pendingEntry(Long id, LocalTime in, LocalTime out, BigDecimal hours) {
        TimeEntry te = new TimeEntry();
        te.setId(id);
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
