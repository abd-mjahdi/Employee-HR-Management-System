package com.example.employeetimetracking.exception;

public class ProjectCodeAlreadyExistsException extends RuntimeException {
    public ProjectCodeAlreadyExistsException(String message) {
        super(message);
    }
}
