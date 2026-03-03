package com.example.employeetimetracking.model.entities;

import com.example.employeetimetracking.model.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="leave_requests")
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_days")
    private BigDecimal totalDays;

    @Column(name = "reason" ,nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_approval_status")
    private Status managerApprovalStatus;

    @ManyToOne
    @JoinColumn(name="manager_approved_by")
    private User managerApprovedBy;

    @Column(name = "manager_approved_at")
    private LocalDateTime managerApprovedAt;

    @Column(name = "manager_notes")
    private String managerNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "hr_approval_status")
    private Status hrApprovalStatus;

    @ManyToOne
    @JoinColumn(name="hr_approved_by")
    private User hrApprovedBy;

    @Column(name = "hr_approved_at")
    private LocalDateTime hrApprovedAt;

    @Column(name = "hr_notes")
    private String hrNotes;

    @CreationTimestamp
    @Column(name = "created_at" ,nullable = false ,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at" ,nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;


}
