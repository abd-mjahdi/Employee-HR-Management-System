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
@Table(name="projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;

    @Column(name="project_name" , length=50 ,nullable = false)
    private String projectName;

    @Column(name="project_code" , length=50 ,nullable = false ,unique = true)
    private String projectCode;

    @Column(name="description")
    private String description;

    @Column(name="is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at" ,nullable = false ,updatable = false)
    private LocalDateTime createdAt;

}
