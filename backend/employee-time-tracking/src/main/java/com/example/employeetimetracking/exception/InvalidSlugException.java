package com.example.employeetimetracking.exception;

public class InvalidSlugException extends RuntimeException {
    public InvalidSlugException(String message) {
        super(message);
    }
}
