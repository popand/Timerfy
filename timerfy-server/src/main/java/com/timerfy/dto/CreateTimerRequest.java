package com.timerfy.dto;

import com.timerfy.model.TimerType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateTimerRequest {
    
    @NotBlank(message = "Timer name cannot be blank")
    @Size(min = 1, max = 100, message = "Timer name must be between 1 and 100 characters")
    private String name;
    
    @Min(value = 1, message = "Timer duration must be at least 1 second")
    private long duration;
    
    @NotNull(message = "Timer type cannot be null")
    private TimerType type = TimerType.COUNTDOWN;
    
    private TimerSettingsDto settings;
    
    public CreateTimerRequest() {}
    
    public CreateTimerRequest(String name, long duration, TimerType type) {
        this.name = name;
        this.duration = duration;
        this.type = type;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public TimerType getType() { return type; }
    public void setType(TimerType type) { this.type = type; }
    
    public TimerSettingsDto getSettings() { return settings; }
    public void setSettings(TimerSettingsDto settings) { this.settings = settings; }
    
    public static class TimerSettingsDto {
        @Min(value = 0, message = "Warning time cannot be negative")
        private long warningTime = 300;
        
        @Min(value = 0, message = "Critical time cannot be negative")
        private long criticalTime = 60;
        
        private Boolean autoReset = false;
        private Boolean playSound = true;
        private Boolean showNotifications = true;
        
        public TimerSettingsDto() {}
        
        public long getWarningTime() { return warningTime; }
        public void setWarningTime(long warningTime) { this.warningTime = warningTime; }
        
        public long getCriticalTime() { return criticalTime; }
        public void setCriticalTime(long criticalTime) { this.criticalTime = criticalTime; }
        
        public Boolean getAutoReset() { return autoReset; }
        public void setAutoReset(Boolean autoReset) { this.autoReset = autoReset; }
        
        public Boolean getPlaySound() { return playSound; }
        public void setPlaySound(Boolean playSound) { this.playSound = playSound; }
        
        public Boolean getShowNotifications() { return showNotifications; }
        public void setShowNotifications(Boolean showNotifications) { this.showNotifications = showNotifications; }
    }
}