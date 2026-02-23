package com.example.employeetimetracking.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="leave_types")
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;

    @Column(name = "type_name" ,unique = true ,nullable = false)
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
}
