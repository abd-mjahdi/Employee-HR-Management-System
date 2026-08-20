package com.example.employeetimetracking.repository;

import com.example.employeetimetracking.model.entities.AuditLog;
import com.example.employeetimetracking.model.enums.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByIdAndCompanyId(Long id, Long companyId);

    List<AuditLog> findAllByCompanyId(Long companyId);

    List<AuditLog> findByCompanyIdAndUserId(Long companyId, Long userId);

    List<AuditLog> findByCompanyIdAndTableNameAndRecordId(Long companyId, String tableName, Long recordId);

    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByTableName(String tableName);
    List<AuditLog> findByActionType(ActionType actionType);
    List<AuditLog> findByTableNameAndRecordId(String tableName, Long recordId);
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

}