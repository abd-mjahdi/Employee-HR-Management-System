package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.Invitation;
import com.example.employeetimetracking.model.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByIdAndCompanyId(Long id, Long companyId);

    Optional<Invitation> findByTokenHashAndStatus(String tokenHash, InvitationStatus status);

    @Query("""
            SELECT i FROM Invitation i
            JOIN FETCH i.company
            LEFT JOIN FETCH i.department
            LEFT JOIN FETCH i.managerMembership mm
            LEFT JOIN FETCH mm.user
            LEFT JOIN FETCH i.invitedByMembership
            WHERE i.tokenHash = :tokenHash
            """)
    Optional<Invitation> findWithDetailsByTokenHash(@Param("tokenHash") String tokenHash);

    List<Invitation> findByCompanyIdAndEmailAndStatus(Long companyId, String email, InvitationStatus status);
}
