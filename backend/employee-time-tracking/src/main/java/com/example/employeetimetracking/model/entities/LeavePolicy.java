package com.example.employeetimetracking.model.entities;

import com.example.employeetimetracking.model.enums.AccrualMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="leave_policies")
public class LeavePolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;

    @OneToOne
    @JoinColumn(name="leave_type_id")
    private LeaveType leaveType;

    @Column(name = "annual_allocation")
    private BigDecimal annualAllocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "accrual_method")
    private AccrualMethod accrualMethod;

    @Column(name = "allows_negative_balance")
    private Boolean allowsNegativeBalance;

    @Column(name = "max_rollover_days")
    private BigDecimal maxRolloverDays;

    @Column(name = "requires_manager_approval")
    private Boolean requiresManagerApproval;

    @Column(name = "requires_hr_approval")
    private Boolean requiresHrApproval;

    @Column(name = "min_notice_days")
    private int minNoticeDays;

    @CreationTimestamp
    @Column(name = "created_at" ,nullable = false ,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
