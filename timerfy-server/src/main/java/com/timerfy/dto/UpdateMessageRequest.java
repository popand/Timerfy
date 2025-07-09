package com.timerfy.dto;

import com.timerfy.model.MessagePriority;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateMessageRequest {
    
    @Size(min = 1, max = 500, message = "Message text must be between 1 and 500 characters")
    private String text;
    
    private Boolean visible;
    
    private MessagePriority priority;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$|^(red|green|blue|yellow|orange|purple|pink|gray|black|white)$", 
             message = "Color must be a valid hex color or predefined color name")
    private String color;
    
    public UpdateMessageRequest() {}
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
    
    public MessagePriority getPriority() { return priority; }
    public void setPriority(MessagePriority priority) { this.priority = priority; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}