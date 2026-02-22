package com.example.employeetimetracking.model.entities;

import com.example.employeetimetracking.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;

    @NotNull
    @Column(name="username",length=50 ,unique = true ,nullable = false)
    private String username;

    @NotNull
    @Column(name="email" ,length=255 ,unique = true ,nullable = false)
    private String email;

    @NotNull
    @Column(name="password_hash" ,nullable = false)
    private String passwordHash;

    @NotNull
    @Column(name="first_name" ,length = 50 ,nullable = false)
    private String firstName;

    @NotNull
    @Column(name="last_name" ,length = 50 ,nullable = false)
    private String lastName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name="user_role" ,nullable = false)
    private UserRole userRole;

    @ManyToOne
    @NotNull
    @JoinColumn(name="department_id" ,nullable = false)
    private Department department;

    @ManyToOne
    @JoinColumn(name="manager_id")
    private User manager;

    @OneToMany(mappedBy = "manager")
    private List<User> teamMembers;

    @NotNull
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at" ,nullable = false ,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
