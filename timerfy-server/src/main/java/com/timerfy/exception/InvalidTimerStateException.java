package com.timerfy.exception;

public class InvalidTimerStateException extends RuntimeException {
    
    public InvalidTimerStateException(String timerId, String currentState, String requestedOperation) {
        super("Timer '" + timerId + "' is in state '" + currentState + "' and cannot perform operation: " + requestedOperation);
    }
    
    public InvalidTimerStateException(String message) {
        super(message);
    }
    
    public InvalidTimerStateException(String message, Throwable cause) {
        super(message, cause);
    }
}