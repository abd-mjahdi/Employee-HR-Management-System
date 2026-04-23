package com.example.employeetimetracking.exception;

public class InvalidTimeEntryException extends RuntimeException {
    public InvalidTimeEntryException(String message) {
        super(message);
    }
}
