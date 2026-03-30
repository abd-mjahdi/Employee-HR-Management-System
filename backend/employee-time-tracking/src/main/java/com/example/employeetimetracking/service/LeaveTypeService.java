package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.LeaveTypeDto;
import com.example.employeetimetracking.mapper.LeaveRequestMapper;
import com.example.employeetimetracking.mapper.LeaveTypeMapper;
import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveTypeService {
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveTypeMapper leaveTypeMapper;
    @Autowired
    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository, LeaveTypeMapper leaveTypeMapper){
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveTypeMapper = leaveTypeMapper;
    }
    public List<LeaveType> getAll(){
        return leaveTypeRepository.findAll();
    }
    public List<LeaveTypeDto> getAllActiveDto(){
        return leaveTypeRepository.findByIsActive(true).stream().map(leaveTypeMapper::toDto).toList();
    }

}
