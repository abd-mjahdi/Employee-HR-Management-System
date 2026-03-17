package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> , JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByDepartmentId(Long id);
    List<User> findByManagerId(Long id);
    List<User> findByUserRole(UserRole userRole);
    List<User> findByIsActive(Boolean isActive);
    List<User> findByDepartmentIdAndIsActive(Long id , Boolean isActive);
    List<User> findByManagerIdAndIsActive(Long id , Boolean isActive);
    Integer countByIsActive(Boolean isActive);

}
