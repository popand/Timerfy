package com.timerfy.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public class Message {
    
    @NotBlank(message = "Message ID cannot be blank")
    private String id;
    
    @NotBlank(message = "Message text cannot be blank")
    @Size(min = 1, max = 500, message = "Message text must be between 1 and 500 characters")
    private String text;
    
    @NotNull(message = "Message visibility cannot be null")
    private Boolean visible = true;
    
    @NotNull(message = "Message timestamp cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    @NotNull(message = "Message priority cannot be null")
    private MessagePriority priority = MessagePriority.NORMAL;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$|^(red|green|blue|yellow|orange|purple|pink|gray|black|white)$", 
             message = "Color must be a valid hex color or predefined color name")
    private String color = "blue";
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime autoHideAt;
    
    private Long displayDuration; // in milliseconds
    
    @NotNull(message = "Auto show setting cannot be null")
    private Boolean autoShow = true;
    
    private String createdBy; // User ID who created the message
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;
    
    public Message() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Message(String text) {
        this();
        this.text = text;
    }
    
    public Message(String text, MessagePriority priority) {
        this(text);
        this.priority = priority;
    }
    
    public Message(String text, MessagePriority priority, String color) {
        this(text, priority);
        this.color = color;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getText() { return text; }
    public void setText(String text) { 
        this.text = text; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { 
        this.visible = visible; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public MessagePriority getPriority() { return priority; }
    public void setPriority(MessagePriority priority) { 
        this.priority = priority; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getColor() { return color; }
    public void setColor(String color) { 
        this.color = color; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getAutoHideAt() { return autoHideAt; }
    public void setAutoHideAt(LocalDateTime autoHideAt) { this.autoHideAt = autoHideAt; }
    
    public Long getDisplayDuration() { return displayDuration; }
    public void setDisplayDuration(Long displayDuration) { 
        this.displayDuration = displayDuration;
        if (displayDuration != null && displayDuration > 0) {
            this.autoHideAt = LocalDateTime.now().plusSeconds(displayDuration / 1000);
        }
    }
    
    public Boolean getAutoShow() { return autoShow; }
    public void setAutoShow(Boolean autoShow) { this.autoShow = autoShow; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public void show() {
        this.visible = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void hide() {
        this.visible = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean shouldAutoHide() {
        return autoHideAt != null && LocalDateTime.now().isAfter(autoHideAt);
    }
    
    public boolean isHighPriority() {
        return priority == MessagePriority.HIGH || priority == MessagePriority.CRITICAL;
    }
    
    public boolean isCriticalPriority() {
        return priority == MessagePriority.CRITICAL;
    }
    
    public void setAutoHideAfter(long seconds) {
        this.autoHideAt = LocalDateTime.now().plusSeconds(seconds);
    }
    
    public long getTimeUntilAutoHide() {
        if (autoHideAt == null) {
            return -1;
        }
        return java.time.Duration.between(LocalDateTime.now(), autoHideAt).getSeconds();
    }
    
    public Message copy() {
        Message copy = new Message();
        copy.id = this.id;
        copy.text = this.text;
        copy.visible = this.visible;
        copy.timestamp = this.timestamp;
        copy.priority = this.priority;
        copy.color = this.color;
        copy.autoHideAt = this.autoHideAt;
        copy.displayDuration = this.displayDuration;
        copy.autoShow = this.autoShow;
        copy.createdBy = this.createdBy;
        copy.updatedAt = this.updatedAt;
        return copy;
    }
}