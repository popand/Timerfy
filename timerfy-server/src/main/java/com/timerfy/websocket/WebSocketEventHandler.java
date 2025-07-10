package com.timerfy.websocket;

import com.timerfy.model.UserRole;
import com.timerfy.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventHandler.class);
    
    // Track active connections: sessionId -> ConnectionInfo
    private final Map<String, ConnectionInfo> activeConnections = new ConcurrentHashMap<>();
    
    // Track room subscriptions: roomId -> Set<sessionId>
    private final Map<String, ConcurrentHashMap<String, ConnectionInfo>> roomSubscriptions = new ConcurrentHashMap<>();
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private RoomService roomService;
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("New WebSocket connection established: {}", sessionId);
        
        // Store connection info
        ConnectionInfo connectionInfo = new ConnectionInfo(sessionId, LocalDateTime.now());
        activeConnections.put(sessionId, connectionInfo);
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("WebSocket connection closed: {}", sessionId);
        
        // Remove from active connections
        ConnectionInfo connectionInfo = activeConnections.remove(sessionId);
        
        if (connectionInfo != null && connectionInfo.getRoomId() != null) {
            // Remove from room subscriptions
            String roomId = connectionInfo.getRoomId();
            removeFromRoomSubscriptions(roomId, sessionId);
            
            // Update room statistics
            updateRoomStatistics(roomId);
            
            // Notify other users in room
            broadcastUserLeft(roomId, sessionId, connectionInfo.getRole());
        }
    }
    
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        if (destination != null && destination.startsWith("/topic/room/")) {
            String roomId = extractRoomIdFromDestination(destination);
            
            if (roomId != null && roomService.roomExists(roomId)) {
                // Get user role from headers
                String roleHeader = headerAccessor.getFirstNativeHeader("role");
                UserRole role = parseUserRole(roleHeader);
                
                // Get client info from headers
                String clientInfo = headerAccessor.getFirstNativeHeader("client-info");
                
                // Update connection info
                ConnectionInfo connectionInfo = activeConnections.get(sessionId);
                if (connectionInfo != null) {
                    connectionInfo.setRoomId(roomId);
                    connectionInfo.setRole(role);
                    connectionInfo.setClientInfo(clientInfo);
                    
                    // Add to room subscriptions
                    addToRoomSubscriptions(roomId, sessionId, connectionInfo);
                    
                    // Update room statistics
                    updateRoomStatistics(roomId);
                    
                    // Send room data to new subscriber
                    sendRoomDataToUser(sessionId, roomId);
                    
                    // Notify other users in room
                    broadcastUserJoined(roomId, sessionId, role);
                    
                    logger.info("User {} joined room {} as {}", sessionId, roomId, role);
                } else {
                    logger.warn("Connection info not found for session: {}", sessionId);
                }
            } else {
                logger.warn("Invalid room ID or room does not exist: {}", roomId);
                // Send error to user
                sendErrorToUser(sessionId, "ROOM_NOT_FOUND", "Room not found or expired");
            }
        }
    }
    
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        ConnectionInfo connectionInfo = activeConnections.get(sessionId);
        if (connectionInfo != null && connectionInfo.getRoomId() != null) {
            String roomId = connectionInfo.getRoomId();
            
            // Remove from room subscriptions
            removeFromRoomSubscriptions(roomId, sessionId);
            
            // Clear room info from connection
            connectionInfo.setRoomId(null);
            connectionInfo.setRole(UserRole.VIEWER);
            
            // Update room statistics
            updateRoomStatistics(roomId);
            
            // Notify other users in room
            broadcastUserLeft(roomId, sessionId, connectionInfo.getRole());
            
            logger.info("User {} left room {}", sessionId, roomId);
        }
    }
    
    private String extractRoomIdFromDestination(String destination) {
        // Extract room ID from destination like "/topic/room/ABC123"
        if (destination.startsWith("/topic/room/")) {
            return destination.substring("/topic/room/".length());
        }
        return null;
    }
    
    private UserRole parseUserRole(String roleHeader) {
        if (roleHeader != null) {
            try {
                return UserRole.valueOf(roleHeader.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid user role: {}", roleHeader);
            }
        }
        return UserRole.VIEWER; // Default role
    }
    
    private void addToRoomSubscriptions(String roomId, String sessionId, ConnectionInfo connectionInfo) {
        roomSubscriptions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                         .put(sessionId, connectionInfo);
    }
    
    private void removeFromRoomSubscriptions(String roomId, String sessionId) {
        ConcurrentHashMap<String, ConnectionInfo> roomSubs = roomSubscriptions.get(roomId);
        if (roomSubs != null) {
            roomSubs.remove(sessionId);
            if (roomSubs.isEmpty()) {
                roomSubscriptions.remove(roomId);
            }
        }
    }
    
    private void updateRoomStatistics(String roomId) {
        ConcurrentHashMap<String, ConnectionInfo> roomSubs = roomSubscriptions.get(roomId);
        int totalUsers = roomSubs != null ? roomSubs.size() : 0;
        int controllers = 0;
        int viewers = 0;
        
        if (roomSubs != null) {
            for (ConnectionInfo info : roomSubs.values()) {
                if (info.getRole() == UserRole.CONTROLLER) {
                    controllers++;
                } else {
                    viewers++;
                }
            }
        }
        
        // Update room statistics in service
        roomService.updateRoomStats(roomId, totalUsers, controllers, viewers);
    }
    
    private void sendRoomDataToUser(String sessionId, String roomId) {
        roomService.getRoomById(roomId).ifPresent(room -> {
            WebSocketMessage message = new WebSocketMessage(
                "ROOM_JOINED",
                room,
                sessionId,
                room.getStats().getConnectedUsers()
            );
            
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/room-data", message);
        });
    }
    
    private void broadcastUserJoined(String roomId, String sessionId, UserRole role) {
        ConcurrentHashMap<String, ConnectionInfo> roomSubs = roomSubscriptions.get(roomId);
        if (roomSubs != null) {
            int totalUsers = roomSubs.size();
            int controllers = (int) roomSubs.values().stream()
                    .filter(info -> info.getRole() == UserRole.CONTROLLER)
                    .count();
            int viewers = totalUsers - controllers;
            
            WebSocketMessage message = new WebSocketMessage(
                "USER_JOINED",
                new UserCountUpdate(totalUsers, controllers, viewers),
                null,
                totalUsers
            );
            
            messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
        }
    }
    
    private void broadcastUserLeft(String roomId, String sessionId, UserRole role) {
        ConcurrentHashMap<String, ConnectionInfo> roomSubs = roomSubscriptions.get(roomId);
        int totalUsers = roomSubs != null ? roomSubs.size() : 0;
        int controllers = 0;
        int viewers = 0;
        
        if (roomSubs != null) {
            controllers = (int) roomSubs.values().stream()
                    .filter(info -> info.getRole() == UserRole.CONTROLLER)
                    .count();
            viewers = totalUsers - controllers;
        }
        
        WebSocketMessage message = new WebSocketMessage(
            "USER_LEFT",
            new UserCountUpdate(totalUsers, controllers, viewers),
            null,
            totalUsers
        );
        
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }
    
    private void sendErrorToUser(String sessionId, String errorCode, String errorMessage) {
        WebSocketMessage message = new WebSocketMessage(
            "ERROR",
            new ErrorDetails(errorCode, errorMessage),
            null,
            0
        );
        
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", message);
    }
    
    public int getConnectedUsersInRoom(String roomId) {
        ConcurrentHashMap<String, ConnectionInfo> roomSubs = roomSubscriptions.get(roomId);
        return roomSubs != null ? roomSubs.size() : 0;
    }
    
    public void broadcastToRoom(String roomId, Object message) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }
    
    // Inner classes for data structures
    public static class ConnectionInfo {
        private final String sessionId;
        private final LocalDateTime connectedAt;
        private String roomId;
        private UserRole role = UserRole.VIEWER;
        private String clientInfo;
        
        public ConnectionInfo(String sessionId, LocalDateTime connectedAt) {
            this.sessionId = sessionId;
            this.connectedAt = connectedAt;
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public LocalDateTime getConnectedAt() { return connectedAt; }
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
        public String getClientInfo() { return clientInfo; }
        public void setClientInfo(String clientInfo) { this.clientInfo = clientInfo; }
    }
    
    public static class UserCountUpdate {
        private final int connectedUsers;
        private final int controllers;
        private final int viewers;
        
        public UserCountUpdate(int connectedUsers, int controllers, int viewers) {
            this.connectedUsers = connectedUsers;
            this.controllers = controllers;
            this.viewers = viewers;
        }
        
        public int getConnectedUsers() { return connectedUsers; }
        public int getControllers() { return controllers; }
        public int getViewers() { return viewers; }
    }
    
    public static class ErrorDetails {
        private final String code;
        private final String message;
        private final LocalDateTime timestamp;
        
        public ErrorDetails(String code, String message) {
            this.code = code;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}