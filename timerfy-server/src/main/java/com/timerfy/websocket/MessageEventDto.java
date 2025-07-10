package com.timerfy.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.timerfy.model.MessagePriority;

import java.time.LocalDateTime;

public class MessageEventDto {
    
    private String id;
    private String text;
    private Boolean visible;
    private MessagePriority priority;
    private String color;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime autoHideAt;
    
    private String createdBy;
    
    public MessageEventDto() {}
    
    public MessageEventDto(String id, String text, Boolean visible, MessagePriority priority, 
                          String color, LocalDateTime timestamp, LocalDateTime autoHideAt, 
                          String createdBy) {
        this.id = id;
        this.text = text;
        this.visible = visible;
        this.priority = priority;
        this.color = color;
        this.timestamp = timestamp;
        this.autoHideAt = autoHideAt;
        this.createdBy = createdBy;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
    
    public MessagePriority getPriority() { return priority; }
    public void setPriority(MessagePriority priority) { this.priority = priority; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public LocalDateTime getAutoHideAt() { return autoHideAt; }
    public void setAutoHideAt(LocalDateTime autoHideAt) { this.autoHideAt = autoHideAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

// DTO for message deletion events
class MessageDeletedEventDto {
    private String messageId;
    private String messageText;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime deletedAt;
    
    public MessageDeletedEventDto(String messageId, String messageText) {
        this.messageId = messageId;
        this.messageText = messageText;
        this.deletedAt = LocalDateTime.now();
    }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}

// DTO for message visibility events
class MessageVisibilityEventDto {
    private String messageId;
    private Boolean visible;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    public MessageVisibilityEventDto(String messageId, Boolean visible) {
        this.messageId = messageId;
        this.visible = visible;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}