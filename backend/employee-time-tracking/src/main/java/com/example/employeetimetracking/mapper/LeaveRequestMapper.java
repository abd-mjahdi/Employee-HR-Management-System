package com.example.employeetimetracking.mapper;

import com.example.employeetimetracking.dto.response.LeaveRequestDto;
import com.example.employeetimetracking.dto.response.LeaveRequestReviewDto;
import com.example.employeetimetracking.model.entities.LeaveRequest;
import org.springframework.stereotype.Component;

@Component
public class LeaveRequestMapper{
    public LeaveRequestDto toDto(LeaveRequest lr){

        return new LeaveRequestDto(
                lr.getId(),
                lr.getUser().getId(),
                lr.getLeaveType().getId(),
                lr.getLeaveType().getTypeName(),
                lr.getStartDate(),
                lr.getEndDate(),
                lr.getTotalDays(),
                lr.getReason(),
                lr.getStatus());
    }

    public LeaveRequestReviewDto toLeaveRequestReviewDto(LeaveRequest lr){
        return new LeaveRequestReviewDto(
                lr.getId(),
                lr.getUser().getFirstName()+" "+lr.getUser().getLastName(),
                lr.getLeaveType().getTypeName(),
                lr.getStartDate(),
                lr.getEndDate(),
                lr.getTotalDays(),
                lr.getReason(),
                lr.getStatus()
                );

    }
}
