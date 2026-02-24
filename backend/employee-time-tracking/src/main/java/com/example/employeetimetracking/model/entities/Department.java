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
@Table(name="departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="department_name" , length=50 ,nullable = false)
    private String departmentName;

    @Column(name="department_code" , length=50 ,nullable = false ,unique = true)
    private String departmentCode;

    @Column(name="is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at" ,nullable = false ,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at" ,nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "department")
    private List<User> users;
}
