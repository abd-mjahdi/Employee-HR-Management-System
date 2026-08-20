package com.example.employeetimetracking.mapper;

import com.example.employeetimetracking.dto.response.AuditLogDto;
import com.example.employeetimetracking.model.entities.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {
    public AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUser() == null ? null : log.getUser().getId(),
                log.getActionType(),
                log.getTableName(),
                log.getRecordId(),
                log.getOldValues(),
                log.getNewValues(),
                log.getIpAddress(),
                log.getTimestamp()
        );
    }
}
