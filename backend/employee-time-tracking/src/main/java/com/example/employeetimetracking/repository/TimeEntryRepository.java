package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.TimeEntry;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long>, JpaSpecificationExecutor<TimeEntry> {

    List<TimeEntry> findByUserId(Long userId);
    List<TimeEntry> findByUserIdAndStatus(Long userId, Status status);

    List<TimeEntry> findByStatus(Status status);
    List<TimeEntry> findByApprovedById(Long approverId);
    List<TimeEntry> findByProjectId(Long projectId);
    List<TimeEntry> findByUserIdAndEntryDateBetweenAndStatus(Long userId, LocalDate startDate, LocalDate endDate ,Status status);
    List<TimeEntry> findByUserIdOrderByEntryDateDesc(Long userId , Pageable limit);
    List<TimeEntry> findByUserIdAndEntryDate(Long userId, LocalDate entryDate);
    List<TimeEntry> findByUserManagerIdAndStatusOrderByCreatedAtAsc(Long managerId, Status status);
    Integer countByUserIdAndStatus(Long userId, Status status);
    Integer countByUserManagerIdAndStatus(Long managerId , Status status);

}