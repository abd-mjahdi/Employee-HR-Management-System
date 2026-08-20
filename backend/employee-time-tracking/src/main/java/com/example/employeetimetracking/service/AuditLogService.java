package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.AuditLogDto;
import com.example.employeetimetracking.exception.AuditLogNotFoundException;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.mapper.AuditLogMapper;
import com.example.employeetimetracking.model.entities.AuditLog;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.ActionType;
import com.example.employeetimetracking.repository.AuditLogRepository;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogMapper auditLogMapper;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository,
                           CompanyRepository companyRepository,
                           AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.companyRepository = companyRepository;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * Persist an audit row for the current tenant. Company is taken from {@link TenantContext}
     * only — callers must not pass a company id.
     */
    @Transactional
    public AuditLog record(User actor,
                           ActionType actionType,
                           String tableName,
                           Long recordId,
                           String oldValues,
                           String newValues,
                           String ipAddress) {
        Long companyId = currentCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new InvalidTenantException("Tenant not found"));

        AuditLog log = new AuditLog();
        log.setCompany(company);
        log.setUser(actor);
        log.setActionType(actionType);
        log.setTableName(tableName);
        log.setRecordId(recordId);
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        log.setIpAddress(ipAddress);
        log.setTimestamp(LocalDateTime.now());
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> listCurrentCompany() {
        return auditLogRepository.findAllByCompanyId(currentCompanyId()).stream()
                .map(auditLogMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditLogDto getById(Long id) {
        AuditLog log = auditLogRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new AuditLogNotFoundException("Audit log does not exist"));
        return auditLogMapper.toDto(log);
    }

    private static Long currentCompanyId() {
        return TenantContext.require().companyId();
    }
}
