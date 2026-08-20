package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.LeaveTypeDto;
import com.example.employeetimetracking.exception.LeaveTypeNotFoundException;
import com.example.employeetimetracking.mapper.LeaveTypeMapper;
import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.repository.LeaveTypeRepository;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaveTypeService {
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveTypeMapper leaveTypeMapper;

    @Autowired
    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository, LeaveTypeMapper leaveTypeMapper) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveTypeMapper = leaveTypeMapper;
    }

    public List<LeaveType> getAll() {
        return leaveTypeRepository.findAllByCompanyId(currentCompanyId());
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeDto> getAllActiveDto() {
        return leaveTypeRepository.findByCompanyIdAndIsActive(currentCompanyId(), true).stream()
                .map(leaveTypeMapper::toDto)
                .toList();
    }

    public LeaveType getById(Long id) {
        return leaveTypeRepository.findByIdAndCompanyIdAndIsActive(id, currentCompanyId(), true)
                .orElseThrow(() -> new LeaveTypeNotFoundException("Leave type Not Found or Inactive"));
    }

    @Transactional(readOnly = true)
    public List<LeaveType> getAllWithPolicy() {
        return leaveTypeRepository.findAllWithPolicyByCompanyId(currentCompanyId());
    }

    private static Long currentCompanyId() {
        return TenantContext.require().companyId();
    }
}
