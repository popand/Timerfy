package com.timerfy.exception;

public class TimerLimitExceededException extends RuntimeException {
    
    public TimerLimitExceededException(String roomId) {
        super("Room '" + roomId + "' has reached maximum number of timers");
    }
    
    public TimerLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}