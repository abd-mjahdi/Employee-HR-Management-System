package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByUserId(Long userId);
    List<TimeEntry> findByUserIdAndStatus(Long userId, Status status);
    List<TimeEntry> findByUserIdAndEntryDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<TimeEntry> findByStatus(Status status);
    List<TimeEntry> findByApprovedBy(Long approverId);
    List<TimeEntry> findByProjectId(Long projectId);

}