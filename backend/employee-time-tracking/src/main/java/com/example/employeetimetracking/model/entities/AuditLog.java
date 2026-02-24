package com.example.employeetimetracking.model.entities;

import com.example.employeetimetracking.model.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private ActionType actionType;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "old_values" ,columnDefinition = "jsonb")
    private String oldValues;

    @Column(name = "new_values" ,columnDefinition = "jsonb")
    private String newValues;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
