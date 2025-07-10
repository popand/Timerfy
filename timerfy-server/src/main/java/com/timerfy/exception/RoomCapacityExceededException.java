package com.timerfy.exception;

public class RoomCapacityExceededException extends RuntimeException {
    
    public RoomCapacityExceededException(String roomId) {
        super("Room '" + roomId + "' has reached maximum capacity");
    }
    
    public RoomCapacityExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}