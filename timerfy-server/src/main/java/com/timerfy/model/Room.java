package com.timerfy.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Room {
    
    @NotBlank(message = "Room ID cannot be blank")
    @Size(min = 6, max = 6, message = "Room ID must be exactly 6 characters")
    private String id;
    
    @NotNull(message = "Creation date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime created;
    
    @NotNull(message = "Last activity date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastActivity;
    
    @NotNull(message = "Expiration date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime expiresAt;
    
    @NotNull(message = "Timers list cannot be null")
    private List<Timer> timers = new ArrayList<>();
    
    @NotNull(message = "Messages list cannot be null")
    private List<Message> messages = new ArrayList<>();
    
    @NotNull(message = "Room settings cannot be null")
    private RoomSettings settings;
    
    @NotNull(message = "Room statistics cannot be null")
    private RoomStats stats;
    
    public static class RoomSettings {
        @Min(value = 1, message = "Maximum timers must be at least 1")
        @Max(value = 50, message = "Maximum timers cannot exceed 50")
        private int maxTimers = 10;
        
        @NotNull(message = "Auto cleanup setting cannot be null")
        private Boolean autoCleanup = true;
        
        @NotNull(message = "Allow viewer messages setting cannot be null")
        private Boolean allowViewerMessages = false;
        
        private String primaryColor = "#3B82F6";
        private String backgroundColor = "#000000";
        private String fontFamily = "sans-serif";
        
        public RoomSettings() {}
        
        public int getMaxTimers() { return maxTimers; }
        public void setMaxTimers(int maxTimers) { this.maxTimers = maxTimers; }
        
        public Boolean getAutoCleanup() { return autoCleanup; }
        public void setAutoCleanup(Boolean autoCleanup) { this.autoCleanup = autoCleanup; }
        
        public Boolean getAllowViewerMessages() { return allowViewerMessages; }
        public void setAllowViewerMessages(Boolean allowViewerMessages) { this.allowViewerMessages = allowViewerMessages; }
        
        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
        
        public String getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
        
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
    }
    
    public static class RoomStats {
        @Min(value = 0, message = "Connected users count cannot be negative")
        private int connectedUsers = 0;
        
        @Min(value = 0, message = "Total controllers count cannot be negative")
        private int totalControllers = 0;
        
        @Min(value = 0, message = "Total viewers count cannot be negative")
        private int totalViewers = 0;
        
        public RoomStats() {}
        
        public int getConnectedUsers() { return connectedUsers; }
        public void setConnectedUsers(int connectedUsers) { this.connectedUsers = connectedUsers; }
        
        public int getTotalControllers() { return totalControllers; }
        public void setTotalControllers(int totalControllers) { this.totalControllers = totalControllers; }
        
        public int getTotalViewers() { return totalViewers; }
        public void setTotalViewers(int totalViewers) { this.totalViewers = totalViewers; }
    }
    
    public Room() {
        this.settings = new RoomSettings();
        this.stats = new RoomStats();
    }
    
    public Room(String id) {
        this.id = id;
        this.created = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(1);
        this.timers = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.settings = new RoomSettings();
        this.stats = new RoomStats();
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }
    
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public List<Timer> getTimers() { return timers; }
    public void setTimers(List<Timer> timers) { this.timers = timers; }
    
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    
    public RoomSettings getSettings() { return settings; }
    public void setSettings(RoomSettings settings) { this.settings = settings; }
    
    public RoomStats getStats() { return stats; }
    public void setStats(RoomStats stats) { this.stats = stats; }
    
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    public void addTimer(Timer timer) {
        if (timers.size() >= settings.getMaxTimers()) {
            throw new IllegalStateException("Maximum number of timers exceeded");
        }
        timers.add(timer);
        updateLastActivity();
    }
    
    public void removeTimer(String timerId) {
        timers.removeIf(timer -> timer.getId().equals(timerId));
        updateLastActivity();
    }
    
    public Timer getTimer(String timerId) {
        return timers.stream()
                .filter(timer -> timer.getId().equals(timerId))
                .findFirst()
                .orElse(null);
    }
    
    public void addMessage(Message message) {
        messages.add(message);
        updateLastActivity();
    }
    
    public void removeMessage(String messageId) {
        messages.removeIf(message -> message.getId().equals(messageId));
        updateLastActivity();
    }
    
    public Message getMessage(String messageId) {
        return messages.stream()
                .filter(message -> message.getId().equals(messageId))
                .findFirst()
                .orElse(null);
    }
}