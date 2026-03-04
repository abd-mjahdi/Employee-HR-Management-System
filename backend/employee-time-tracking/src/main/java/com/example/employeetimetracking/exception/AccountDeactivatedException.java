package com.example.employeetimetracking.exception;

public class AccountDeactivatedException extends AuthenticationException{
    public AccountDeactivatedException(String message){
        super(message);
    }
}
