package com.timerfy.exception;

public class TimerNotFoundException extends RuntimeException {
    
    public TimerNotFoundException(String timerId) {
        super("Timer with ID '" + timerId + "' not found");
    }
    
    public TimerNotFoundException(String roomId, String timerId) {
        super("Timer with ID '" + timerId + "' not found in room '" + roomId + "'");
    }
    
    public TimerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}