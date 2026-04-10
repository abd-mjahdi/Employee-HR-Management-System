package com.example.employeetimetracking.exception;

public class InactiveLeaveTypeException extends RuntimeException {
    public InactiveLeaveTypeException(String message) {
        super(message);
    }
}
