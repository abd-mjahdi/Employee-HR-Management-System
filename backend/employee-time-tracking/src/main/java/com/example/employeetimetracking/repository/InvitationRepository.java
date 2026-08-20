package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.Invitation;
import com.example.employeetimetracking.model.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByIdAndCompanyId(Long id, Long companyId);

    Optional<Invitation> findByTokenHashAndStatus(String tokenHash, InvitationStatus status);

    List<Invitation> findByCompanyIdAndEmailAndStatus(Long companyId, String email, InvitationStatus status);
}
