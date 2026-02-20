package com.example.employeetimetracking.model.entities;

import com.example.employeetimetracking.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="users")
    private Integer Id;

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
}
