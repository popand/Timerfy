package com.timerfy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UpdateTimerRequest {
    
    @Size(min = 1, max = 100, message = "Timer name must be between 1 and 100 characters")
    private String name;
    
    @Min(value = 1, message = "Timer duration must be at least 1 second")
    private Long duration;
    
    private CreateTimerRequest.TimerSettingsDto settings;
    
    public UpdateTimerRequest() {}
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    
    public CreateTimerRequest.TimerSettingsDto getSettings() { return settings; }
    public void setSettings(CreateTimerRequest.TimerSettingsDto settings) { this.settings = settings; }
}