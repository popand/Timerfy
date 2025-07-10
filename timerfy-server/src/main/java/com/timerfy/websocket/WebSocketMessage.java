package com.timerfy.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {
    
    private String type;
    private Object data;
    private String clientId;
    private Integer connectedUsers;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public WebSocketMessage(String type, Object data) {
        this();
        this.type = type;
        this.data = data;
    }
    
    public WebSocketMessage(String type, Object data, String clientId, Integer connectedUsers) {
        this();
        this.type = type;
        this.data = data;
        this.clientId = clientId;
        this.connectedUsers = connectedUsers;
    }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public Integer getConnectedUsers() { return connectedUsers; }
    public void setConnectedUsers(Integer connectedUsers) { this.connectedUsers = connectedUsers; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}