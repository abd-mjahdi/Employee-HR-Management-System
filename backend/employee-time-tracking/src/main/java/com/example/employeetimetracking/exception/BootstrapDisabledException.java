package com.example.employeetimetracking.exception;

public class BootstrapDisabledException extends RuntimeException {
    public BootstrapDisabledException() {
        super("Not found");
    }
}
