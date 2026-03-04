package com.example.employeetimetracking.exception;

public class EmailAlreadyRegisteredException extends AuthenticationException{
    public EmailAlreadyRegisteredException(String message){
        super(message);
    }
}
