package com.example.employeetimetracking.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "leave_types",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "type_name"})
)
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "type_name" ,nullable = false)
    private String typeName;

    @Column(name="description")
    private String description;

    @Column(name="is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at" ,nullable = false ,updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "leaveType")
    private LeavePolicy leavePolicy;

    @OneToMany(mappedBy = "leaveType")
    private List<LeaveBalance> leaveBalanceList;

    @OneToMany(mappedBy = "leaveType")
    private List<LeaveRequest> leaveRequestList;


}
