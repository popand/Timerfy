package com.timerfy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

public class TimerControlRequest {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime startTime;
    
    @Min(value = 1, message = "New duration must be at least 1 second")
    private Long newDuration;
    
    private Long adjustment; // Can be positive or negative
    
    public TimerControlRequest() {}
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public Long getNewDuration() { return newDuration; }
    public void setNewDuration(Long newDuration) { this.newDuration = newDuration; }
    
    public Long getAdjustment() { return adjustment; }
    public void setAdjustment(Long adjustment) { this.adjustment = adjustment; }
}