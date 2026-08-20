package com.example.employeetimetracking.exception;

public class LoginRateLimitedException extends RuntimeException {
    public LoginRateLimitedException(String message) {
        super(message);
    }
}
