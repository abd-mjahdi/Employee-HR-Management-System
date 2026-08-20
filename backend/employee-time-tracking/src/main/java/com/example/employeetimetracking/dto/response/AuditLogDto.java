package com.example.employeetimetracking.dto.response;

import com.example.employeetimetracking.model.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private Long userId;
    private ActionType actionType;
    private String tableName;
    private Long recordId;
    private String oldValues;
    private String newValues;
    private String ipAddress;
    private LocalDateTime timestamp;
}
