package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.EmailOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmailOutbox e WHERE e.id = :id")
    Optional<EmailOutbox> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT * FROM email_outbox
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EmailOutbox> lockDue(@Param("now") LocalDateTime now, @Param("batchSize") int batchSize);
}
