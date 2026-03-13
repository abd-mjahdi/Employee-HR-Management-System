package com.example.employeetimetracking.service;

import com.example.employeetimetracking.model.entities.LeaveType;
import com.example.employeetimetracking.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveTypeService {
    private final LeaveTypeRepository leaveTypeRepository;
    @Autowired
    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository){
        this.leaveTypeRepository = leaveTypeRepository;
    }
    public List<LeaveType> getAll(){
        return leaveTypeRepository.findAll();
    }



}
