package com.example.employeetimetracking.exception;

import com.example.employeetimetracking.dto.response.ErrorResponseDto;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.dto.response.RegisterResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<LoginResponseDto> handleInvalidCredentials(InvalidCredentialsException exception) {
        LoginResponseDto response = new LoginResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<LoginResponseDto> handleAccountDeactivation(AccountDeactivatedException exception) {
        LoginResponseDto response = new LoginResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<RegisterResponseDto> handleDepartmentNotFound(DepartmentNotFoundException exception) {
        RegisterResponseDto response = new RegisterResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<RegisterResponseDto> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        RegisterResponseDto response = new RegisterResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidUserRoleException.class)
    public ResponseEntity<RegisterResponseDto> handleInvalidUserRole(InvalidUserRoleException exception) {
        RegisterResponseDto response = new RegisterResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<RegisterResponseDto> handleWeakPassword(WeakPasswordException exception) {
        RegisterResponseDto response = new RegisterResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserDetailsNotFound(UserNotFoundException exception){
        ErrorResponseDto response = new ErrorResponseDto("User not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException exception) {
        ErrorResponseDto response = new ErrorResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

}