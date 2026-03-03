package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    User findByUsername(String username);
    User findByEmail(String email);
    Boolean existsByEmail(String email);
    List<User> findByDepartmentId(Long id);
    List<User> findByManagerId(Long id);
    List<User> findByUserRole(UserRole userRole);
    List<User> findByIsActive(Boolean isActive);
    List<User> findByDepartmentIdAndIsActive(Long id , Boolean isActive);
    List<User> findByManagerIdAndIsActive(Long id , Boolean isActive);

}
