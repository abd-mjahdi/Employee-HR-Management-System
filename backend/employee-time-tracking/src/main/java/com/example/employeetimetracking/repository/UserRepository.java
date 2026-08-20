package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Identity lookups ({@code findByEmail}, {@code existsByUsername}) stay global.
 * Tenant people lists go through {@link CompanyMembershipRepository}.
 */
@Repository
public interface UserRepository extends JpaRepository<User,Long> , JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByIsActive(Boolean isActive);
    Integer countByIsActive(Boolean isActive);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByUsernameAndIdNot(String username, Long id);
}
