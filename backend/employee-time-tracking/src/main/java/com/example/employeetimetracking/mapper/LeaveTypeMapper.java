package com.example.employeetimetracking.mapper;

import com.example.employeetimetracking.dto.response.LeaveTypeDto;
import com.example.employeetimetracking.model.entities.LeaveType;
import org.springframework.stereotype.Component;

@Component
public class LeaveTypeMapper {
    public LeaveTypeDto toDto(LeaveType leaveType){
        return new LeaveTypeDto(
                leaveType.getId(),
                leaveType.getTypeName(),
                leaveType.getDescription(),
                leaveType.getIsActive());
    }
}
