package com.timerfy.exception;

public class RoomNotFoundException extends RuntimeException {
    
    public RoomNotFoundException(String roomId) {
        super("Room with ID '" + roomId + "' not found or has expired");
    }
    
    public RoomNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}