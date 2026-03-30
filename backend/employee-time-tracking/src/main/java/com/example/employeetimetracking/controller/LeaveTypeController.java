package com.example.employeetimetracking.controller;

import com.example.employeetimetracking.dto.response.LeaveTypeDto;
import com.example.employeetimetracking.service.LeaveTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/leave-types")
public class LeaveTypeController {
    private final LeaveTypeService leaveTypeService;
    @Autowired
    public LeaveTypeController(LeaveTypeService leaveTypeService){
        this.leaveTypeService = leaveTypeService;
    }

    @GetMapping
    public ResponseEntity<List<LeaveTypeDto>> getAllDto(){
        List<LeaveTypeDto> leaveTypes = leaveTypeService.getAllActiveDto();
        return ResponseEntity.ok(leaveTypes);
    }
}
