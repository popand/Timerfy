package com.timerfy.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.timerfy.model.Timer;
import com.timerfy.model.TimerState;
import com.timerfy.model.TimerType;

import java.time.LocalDateTime;

public class TimerEventDto {
    
    private String id;
    private String name;
    private long duration;
    private long currentTime;
    private TimerState state;
    private TimerType type;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime startedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime completedAt;
    
    private Timer.TimerSettings settings;
    
    public TimerEventDto() {}
    
    public TimerEventDto(String id, String name, long duration, long currentTime, 
                        TimerState state, TimerType type, LocalDateTime createdAt, 
                        LocalDateTime startedAt, LocalDateTime completedAt, 
                        Timer.TimerSettings settings) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.currentTime = currentTime;
        this.state = state;
        this.type = type;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.settings = settings;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
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
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public Timer.TimerSettings getSettings() { return settings; }
    public void setSettings(Timer.TimerSettings settings) { this.settings = settings; }
}

// Lightweight DTO for timer tick events
class TimerTickEventDto {
    private String timerId;
    private long currentTime;
    private TimerState state;
    
    public TimerTickEventDto(String timerId, long currentTime, TimerState state) {
        this.timerId = timerId;
        this.currentTime = currentTime;
        this.state = state;
    }
    
    public String getTimerId() { return timerId; }
    public void setTimerId(String timerId) { this.timerId = timerId; }
    
    public long getCurrentTime() { return currentTime; }
    public void setCurrentTime(long currentTime) { this.currentTime = currentTime; }
    
    public TimerState getState() { return state; }
    public void setState(TimerState state) { this.state = state; }
}

// DTO for timer control events (start, stop, pause, reset)
class TimerControlEventDto {
    private String timerId;
    private TimerState state;
    private long currentTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    public TimerControlEventDto(String timerId, TimerState state, long currentTime, LocalDateTime timestamp) {
        this.timerId = timerId;
        this.state = state;
        this.currentTime = currentTime;
        this.timestamp = timestamp;
    }
    
    public String getTimerId() { return timerId; }
    public void setTimerId(String timerId) { this.timerId = timerId; }
    
    public TimerState getState() { return state; }
    public void setState(TimerState state) { this.state = state; }
    
    public long getCurrentTime() { return currentTime; }
    public void setCurrentTime(long currentTime) { this.currentTime = currentTime; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

// DTO for timer deletion events
class TimerDeletedEventDto {
    private String timerId;
    private String timerName;
    
    public TimerDeletedEventDto(String timerId, String timerName) {
        this.timerId = timerId;
        this.timerName = timerName;
    }
    
    public String getTimerId() { return timerId; }
    public void setTimerId(String timerId) { this.timerId = timerId; }
    
    public String getTimerName() { return timerName; }
    public void setTimerName(String timerName) { this.timerName = timerName; }
}

// DTO for timer warning/critical alerts
class TimerAlertEventDto {
    private String timerId;
    private String alertType;
    private long timeRemaining;
    private long threshold;
    private String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    public TimerAlertEventDto(String timerId, String alertType, long timeRemaining, long threshold, String message) {
        this.timerId = timerId;
        this.alertType = alertType;
        this.timeRemaining = timeRemaining;
        this.threshold = threshold;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getTimerId() { return timerId; }
    public void setTimerId(String timerId) { this.timerId = timerId; }
    
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    
    public long getTimeRemaining() { return timeRemaining; }
    public void setTimeRemaining(long timeRemaining) { this.timeRemaining = timeRemaining; }
    
    public long getThreshold() { return threshold; }
    public void setThreshold(long threshold) { this.threshold = threshold; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

// DTO for timer completion events
class TimerCompletedEventDto {
    private String timerId;
    private String timerName;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime completedAt;
    
    private Boolean playSound;
    private Boolean autoReset;
    
    public TimerCompletedEventDto(String timerId, String timerName, LocalDateTime completedAt, 
                                 Boolean playSound, Boolean autoReset) {
        this.timerId = timerId;
        this.timerName = timerName;
        this.completedAt = completedAt;
        this.playSound = playSound;
        this.autoReset = autoReset;
    }
    
    public String getTimerId() { return timerId; }
    public void setTimerId(String timerId) { this.timerId = timerId; }
    
    public String getTimerName() { return timerName; }
    public void setTimerName(String timerName) { this.timerName = timerName; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public Boolean getPlaySound() { return playSound; }
    public void setPlaySound(Boolean playSound) { this.playSound = playSound; }
    
    public Boolean getAutoReset() { return autoReset; }
    public void setAutoReset(Boolean autoReset) { this.autoReset = autoReset; }
}