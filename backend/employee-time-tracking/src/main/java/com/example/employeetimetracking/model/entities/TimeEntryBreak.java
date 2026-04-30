package com.example.employeetimetracking.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "time_entry_breaks")
public class TimeEntryBreak {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "time_entry_id", nullable = false)
    private TimeEntry timeEntry;

    @Column(name = "break_start", nullable = false)
    private LocalTime breakStart;

    @Column(name = "break_end", nullable = false)
    private LocalTime breakEnd;

    @Column(name = "is_unpaid", nullable = false)
    private Boolean isUnpaid = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

