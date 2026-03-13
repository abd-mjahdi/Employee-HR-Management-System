package com.example.employeetimetracking.model.entities;

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
@Table(name="leave_balances" , uniqueConstraints = @UniqueConstraint(columnNames ={"user_id", "leave_type_id", "year"} ))
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    @Column(name = "year")
    private short year;

    @Column(name = "current_balance")
    private BigDecimal currentBalance;

    @Column(name = "last_accrual_date")
    private LocalDate lastAccrualDate;

    @CreationTimestamp
    @Column(name = "created_at" ,nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
