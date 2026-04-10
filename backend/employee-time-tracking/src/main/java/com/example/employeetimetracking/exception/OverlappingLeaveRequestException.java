package com.example.employeetimetracking.exception;

public class OverlappingLeaveRequestException extends RuntimeException {
    public OverlappingLeaveRequestException(String message) {
        super(message);
    }
}
