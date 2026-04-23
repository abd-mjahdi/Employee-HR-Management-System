package com.example.employeetimetracking.handler;

import com.example.employeetimetracking.dto.response.ErrorResponseDto;
import com.example.employeetimetracking.dto.response.LoginResponseDto;
import com.example.employeetimetracking.dto.response.RegisterResponseDto;
import com.example.employeetimetracking.exception.*;
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

    @ExceptionHandler({
            EmailAlreadyRegisteredException.class,
            UsernameAlreadyExists.class
    })
    public ResponseEntity<ErrorResponseDto> handleConflict(RuntimeException exception) {
        ErrorResponseDto response = new ErrorResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({
            InvalidUserRoleException.class,
            WeakPasswordException.class,
            InvalidEmployeeManagerException.class,
            InvalidManagerSupervisorException.class,
            NegativeLeaveBalanceException.class,
            InvalidDateRangeException.class,
            InsufficientNoticePeriodException.class,
            OverlappingLeaveRequestException.class,
            InactiveLeaveTypeException.class,
            NullBalanceException.class,
            InsufficientLeaveBalanceException.class,
            LeaveApprovalException.class,
            InvalidLeaveRequestException.class,
            ProjectNotFoundException.class,
            InvalidTimeEntryException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequest(RuntimeException exception) {
        ErrorResponseDto response = new ErrorResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            LeavePolicyNotFoundException.class,
            LeaveBalanceNotFoundException.class,
            LeaveTypeNotFoundException.class,
            LeaveRequestNotFoundException.class
    })
    public ResponseEntity<ErrorResponseDto> handleNotFound(RuntimeException exception) {
        ErrorResponseDto response = new ErrorResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException exception) {
        ErrorResponseDto response = new ErrorResponseDto(exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

}