package com.example.employeetimetracking.unit.service.timeentry;

import com.example.employeetimetracking.dto.request.CreateTimeEntryDto;
import com.example.employeetimetracking.dto.response.TimeEntryDto;
import com.example.employeetimetracking.mapper.TimeEntryMapper;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

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
        verify(timeEntryRepository,times(1)).save(any(TimeEntry.class));
        verify(timeEntryMapper).toDto(any(TimeEntry.class));
    }
}
