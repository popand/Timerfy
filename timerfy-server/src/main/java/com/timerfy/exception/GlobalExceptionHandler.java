package com.timerfy.exception;

import com.timerfy.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleRoomNotFound(RoomNotFoundException ex, WebRequest request) {
        logger.warn("Room not found: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("ROOM_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(TimerNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleTimerNotFound(TimerNotFoundException ex, WebRequest request) {
        logger.warn("Timer not found: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("TIMER_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotFound(MessageNotFoundException ex, WebRequest request) {
        logger.warn("Message not found: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("MESSAGE_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(RoomCapacityExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleRoomCapacityExceeded(RoomCapacityExceededException ex, WebRequest request) {
        logger.warn("Room capacity exceeded: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("ROOM_CAPACITY_EXCEEDED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    @ExceptionHandler(TimerLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleTimerLimitExceeded(TimerLimitExceededException ex, WebRequest request) {
        logger.warn("Timer limit exceeded: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("TIMER_LIMIT_EXCEEDED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    @ExceptionHandler(InvalidTimerStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidTimerState(InvalidTimerStateException ex, WebRequest request) {
        logger.warn("Invalid timer state: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("INVALID_TIMER_STATE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String errorMessage = "Validation failed: " + errors.entrySet().stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue())
                .collect(Collectors.joining(", "));
        
        logger.warn("Validation error: {}", errorMessage);
        ApiResponse<Object> response = ApiResponse.error("VALIDATION_ERROR", errorMessage, errors.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        logger.warn("Constraint violation: {}", errorMessage);
        ApiResponse<Object> response = ApiResponse.error("VALIDATION_ERROR", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("INVALID_ARGUMENT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException ex, WebRequest request) {
        logger.warn("Illegal state: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error("INVALID_STATE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unexpected error occurred", ex);
        ApiResponse<Object> response = ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}