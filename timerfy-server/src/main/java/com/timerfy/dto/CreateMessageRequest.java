package com.timerfy.dto;

import com.timerfy.model.MessagePriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateMessageRequest {
    
    @NotBlank(message = "Message text cannot be blank")
    @Size(min = 1, max = 500, message = "Message text must be between 1 and 500 characters")
    private String text;
    
    @NotNull(message = "Message priority cannot be null")
    private MessagePriority priority = MessagePriority.NORMAL;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$|^(red|green|blue|yellow|orange|purple|pink|gray|black|white)$", 
             message = "Color must be a valid hex color or predefined color name")
    private String color = "blue";
    
    private Boolean autoShow = true;
    
    @Min(value = 1000, message = "Display duration must be at least 1000ms (1 second)")
    private Long duration; // in milliseconds
    
    public CreateMessageRequest() {}
    
    public CreateMessageRequest(String text) {
        this.text = text;
    }
    
    public CreateMessageRequest(String text, MessagePriority priority) {
        this.text = text;
        this.priority = priority;
    }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public MessagePriority getPriority() { return priority; }
    public void setPriority(MessagePriority priority) { this.priority = priority; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public Boolean getAutoShow() { return autoShow; }
    public void setAutoShow(Boolean autoShow) { this.autoShow = autoShow; }
    
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
}