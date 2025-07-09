package com.timerfy.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public class Timer {
    
    @NotBlank(message = "Timer ID cannot be blank")
    private String id;
    
    @NotBlank(message = "Timer name cannot be blank")
    @Size(min = 1, max = 100, message = "Timer name must be between 1 and 100 characters")
    private String name;
    
    @Min(value = 1, message = "Timer duration must be at least 1 second")
    private long duration;
    
    @Min(value = 0, message = "Current time cannot be negative")
    private long currentTime;
    
    @NotNull(message = "Timer state cannot be null")
    private TimerState state = TimerState.STOPPED;
    
    @NotNull(message = "Timer type cannot be null")
    private TimerType type = TimerType.COUNTDOWN;
    
    @NotNull(message = "Creation date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime startedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime pausedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime completedAt;
    
    @NotNull(message = "Timer settings cannot be null")
    private TimerSettings settings;
    
    private long pausedDuration = 0;
    
    public static class TimerSettings {
        @Min(value = 0, message = "Warning time cannot be negative")
        private long warningTime = 300; // 5 minutes
        
        @Min(value = 0, message = "Critical time cannot be negative")
        private long criticalTime = 60;  // 1 minute
        
        @NotNull(message = "Auto reset setting cannot be null")
        private Boolean autoReset = false;
        
        @NotNull(message = "Play sound setting cannot be null")
        private Boolean playSound = true;
        
        @NotNull(message = "Show notifications setting cannot be null")
        private Boolean showNotifications = true;
        
        public TimerSettings() {}
        
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
    
    public Timer() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.settings = new TimerSettings();
    }
    
    public Timer(String name, long duration, TimerType type) {
        this();
        this.name = name;
        this.duration = duration;
        this.type = type;
        this.currentTime = (type == TimerType.COUNTDOWN) ? duration : 0;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { 
        this.duration = duration; 
        if (type == TimerType.COUNTDOWN && state == TimerState.STOPPED) {
            this.currentTime = duration;
        }
    }
    
    public long getCurrentTime() { return currentTime; }
    public void setCurrentTime(long currentTime) { this.currentTime = currentTime; }
    
    public TimerState getState() { return state; }
    public void setState(TimerState state) { this.state = state; }
    
    public TimerType getType() { return type; }
    public void setType(TimerType type) { this.type = type; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public TimerSettings getSettings() { return settings; }
    public void setSettings(TimerSettings settings) { this.settings = settings; }
    
    public long getPausedDuration() { return pausedDuration; }
    public void setPausedDuration(long pausedDuration) { this.pausedDuration = pausedDuration; }
    
    public synchronized void start() {
        if (state == TimerState.STOPPED || state == TimerState.PAUSED) {
            state = TimerState.RUNNING;
            startedAt = LocalDateTime.now();
            
            if (pausedAt != null) {
                pausedDuration += java.time.Duration.between(pausedAt, LocalDateTime.now()).getSeconds();
                pausedAt = null;
            }
        }
    }
    
    public synchronized void pause() {
        if (state == TimerState.RUNNING) {
            state = TimerState.PAUSED;
            pausedAt = LocalDateTime.now();
        }
    }
    
    public synchronized void stop() {
        state = TimerState.STOPPED;
        startedAt = null;
        pausedAt = null;
        pausedDuration = 0;
        currentTime = (type == TimerType.COUNTDOWN) ? duration : 0;
    }
    
    public synchronized void reset() {
        stop();
    }
    
    public synchronized void reset(long newDuration) {
        this.duration = newDuration;
        reset();
    }
    
    public synchronized void adjustTime(long adjustment) {
        if (type == TimerType.COUNTDOWN) {
            currentTime = Math.max(0, currentTime + adjustment);
        } else {
            currentTime = Math.max(0, currentTime + adjustment);
        }
    }
    
    public synchronized void tick() {
        if (state != TimerState.RUNNING) {
            return;
        }
        
        if (type == TimerType.COUNTDOWN) {
            currentTime--;
            if (currentTime <= 0) {
                currentTime = 0;
                state = TimerState.COMPLETED;
                completedAt = LocalDateTime.now();
            }
        } else {
            currentTime++;
        }
    }
    
    public boolean isInWarningState() {
        return type == TimerType.COUNTDOWN && 
               currentTime <= settings.getWarningTime() && 
               currentTime > settings.getCriticalTime();
    }
    
    public boolean isInCriticalState() {
        return type == TimerType.COUNTDOWN && 
               currentTime <= settings.getCriticalTime() && 
               currentTime > 0;
    }
    
    public boolean isCompleted() {
        return state == TimerState.COMPLETED || 
               (type == TimerType.COUNTDOWN && currentTime <= 0);
    }
    
    public long getElapsedTime() {
        if (startedAt == null) {
            return 0;
        }
        
        LocalDateTime endTime = (state == TimerState.RUNNING) ? LocalDateTime.now() : 
                               (pausedAt != null) ? pausedAt : completedAt;
        
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        return java.time.Duration.between(startedAt, endTime).getSeconds() - pausedDuration;
    }
    
    public long getRemainingTime() {
        return (type == TimerType.COUNTDOWN) ? currentTime : duration - currentTime;
    }
}