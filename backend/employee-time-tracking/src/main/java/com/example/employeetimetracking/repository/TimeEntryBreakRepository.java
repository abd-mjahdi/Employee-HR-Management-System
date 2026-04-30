package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.TimeEntryBreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeEntryBreakRepository extends JpaRepository<TimeEntryBreak, Long> {
    List<TimeEntryBreak> findByTimeEntryIdOrderByBreakStartAsc(Long timeEntryId);
    List<TimeEntryBreak> findByTimeEntryIdIn(List<Long> timeEntryIds);
}

