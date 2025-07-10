package com.timerfy.exception;

public class MessageNotFoundException extends RuntimeException {
    
    public MessageNotFoundException(String messageId) {
        super("Message with ID '" + messageId + "' not found");
    }
    
    public MessageNotFoundException(String roomId, String messageId) {
        super("Message with ID '" + messageId + "' not found in room '" + roomId + "'");
    }
    
    public MessageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}