package com.example.employeetimetracking.exception;

public class InsufficientNoticePeriodException extends RuntimeException {
    public InsufficientNoticePeriodException(String message) {
        super(message);
    }
}
