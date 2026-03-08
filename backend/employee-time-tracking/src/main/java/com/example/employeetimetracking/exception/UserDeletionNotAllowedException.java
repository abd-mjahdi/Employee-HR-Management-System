package com.example.employeetimetracking.exception;

public class UserDeletionNotAllowedException extends RuntimeException{
    public UserDeletionNotAllowedException(String message){
        super(message);
    }
}
