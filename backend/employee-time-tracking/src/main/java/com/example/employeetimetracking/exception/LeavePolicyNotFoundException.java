package com.example.employeetimetracking.exception;

public class LeavePolicyNotFoundException extends RuntimeException {
    public LeavePolicyNotFoundException(String message) {
        super(message);
    }
}
