package com.example.employeetimetracking.exception;

public class InvalidCredentialsException extends AuthenticationException{
    public InvalidCredentialsException(String message){
        super(message);
    }
}
