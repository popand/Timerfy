package com.timerfy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private T data;
    private ErrorDetails error;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ApiResponse(boolean success) {
        this();
        this.success = success;
    }
    
    public ApiResponse(T data) {
        this();
        this.success = true;
        this.data = data;
    }
    
    public ApiResponse(ErrorDetails error) {
        this();
        this.success = false;
        this.error = error;
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }
    
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true);
    }
    
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(new ErrorDetails(code, message));
    }
    
    public static <T> ApiResponse<T> error(String code, String message, String details) {
        return new ApiResponse<>(new ErrorDetails(code, message, details));
    }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public ErrorDetails getError() { return error; }
    public void setError(ErrorDetails error) { this.error = error; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public static class ErrorDetails {
        private String code;
        private String message;
        private String details;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime timestamp;
        
        public ErrorDetails() {
            this.timestamp = LocalDateTime.now();
        }
        
        public ErrorDetails(String code, String message) {
            this();
            this.code = code;
            this.message = message;
        }
        
        public ErrorDetails(String code, String message, String details) {
            this(code, message);
            this.details = details;
        }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}