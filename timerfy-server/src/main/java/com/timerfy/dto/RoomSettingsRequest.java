package com.timerfy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class RoomSettingsRequest {
    
    @Min(value = 1, message = "Maximum timers must be at least 1")
    @Max(value = 50, message = "Maximum timers cannot exceed 50")
    private Integer maxTimers;
    
    private Boolean allowViewerMessages;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Primary color must be a valid hex color")
    private String primaryColor;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Background color must be a valid hex color")
    private String backgroundColor;
    
    @Pattern(regexp = "^(sans-serif|serif|monospace|cursive|fantasy)$", 
             message = "Font family must be one of: sans-serif, serif, monospace, cursive, fantasy")
    private String fontFamily;
    
    public RoomSettingsRequest() {}
    
    public Integer getMaxTimers() { return maxTimers; }
    public void setMaxTimers(Integer maxTimers) { this.maxTimers = maxTimers; }
    
    public Boolean getAllowViewerMessages() { return allowViewerMessages; }
    public void setAllowViewerMessages(Boolean allowViewerMessages) { this.allowViewerMessages = allowViewerMessages; }
    
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
    
    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
}