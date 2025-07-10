package com.timerfy.controller;

import com.timerfy.dto.ApiResponse;
import com.timerfy.dto.RoomSettingsRequest;
import com.timerfy.exception.RoomNotFoundException;
import com.timerfy.model.Room;
import com.timerfy.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/rooms")
@Tag(name = "Room Management", description = "APIs for managing timer rooms")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class RoomController {
    
    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);
    
    @Autowired
    private RoomService roomService;
    
    @PostMapping
    @Operation(
        summary = "Create a new room",
        description = "Creates a new timer room with a unique ID and default settings"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Room created successfully",
            content = @Content(schema = @Schema(implementation = Room.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<ApiResponse<Room>> createRoom() {
        try {
            Room room = roomService.createRoom();
            logger.info("Created new room: {}", room.getId());
            
            ApiResponse<Room> response = ApiResponse.success(room);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Failed to create room", e);
            throw e;
        }
    }
    
    @GetMapping("/{roomId}")
    @Operation(
        summary = "Get room details",
        description = "Retrieves complete room information including timers, messages, and settings"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Room found",
            content = @Content(schema = @Schema(implementation = Room.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found or expired"
        )
    })
    public ResponseEntity<ApiResponse<Room>> getRoomById(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId) {
        
        Optional<Room> roomOpt = roomService.getRoomById(roomId);
        
        if (roomOpt.isEmpty()) {
            throw new RoomNotFoundException(roomId);
        }
        
        Room room = roomOpt.get();
        roomService.touchRoom(roomId); // Update last activity
        
        logger.debug("Retrieved room: {}", roomId);
        ApiResponse<Room> response = ApiResponse.success(room);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{roomId}/status")
    @Operation(
        summary = "Get room status",
        description = "Retrieves lightweight room status information for health checks"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Room status retrieved"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found or expired"
        )
    })
    public ResponseEntity<ApiResponse<RoomStatus>> getRoomStatus(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId) {
        
        Optional<Room> roomOpt = roomService.getRoomById(roomId);
        
        if (roomOpt.isEmpty()) {
            throw new RoomNotFoundException(roomId);
        }
        
        Room room = roomOpt.get();
        RoomStatus status = new RoomStatus(
            true,
            true,
            room.getStats().getConnectedUsers(),
            room.getLastActivity()
        );
        
        logger.debug("Retrieved room status: {}", roomId);
        ApiResponse<RoomStatus> response = ApiResponse.success(status);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{roomId}")
    @Operation(
        summary = "Delete a room",
        description = "Permanently deletes a room and all its associated data"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Room deleted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"
        )
    })
    public ResponseEntity<ApiResponse<String>> deleteRoom(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        boolean deleted = roomService.deleteRoom(roomId);
        
        if (deleted) {
            logger.info("Deleted room: {}", roomId);
            ApiResponse<String> response = ApiResponse.success("Room deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Failed to delete room: {}", roomId);
            ApiResponse<String> response = ApiResponse.error("DELETE_FAILED", "Failed to delete room");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PutMapping("/{roomId}/settings")
    @Operation(
        summary = "Update room settings",
        description = "Updates room configuration settings"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Settings updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid settings data"
        )
    })
    public ResponseEntity<ApiResponse<Room>> updateRoomSettings(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Valid @RequestBody RoomSettingsRequest settingsRequest) {
        
        Optional<Room> roomOpt = roomService.getRoomById(roomId);
        
        if (roomOpt.isEmpty()) {
            throw new RoomNotFoundException(roomId);
        }
        
        Room room = roomOpt.get();
        Room.RoomSettings settings = room.getSettings();
        
        // Update settings if provided
        if (settingsRequest.getMaxTimers() != null) {
            settings.setMaxTimers(settingsRequest.getMaxTimers());
        }
        if (settingsRequest.getAllowViewerMessages() != null) {
            settings.setAllowViewerMessages(settingsRequest.getAllowViewerMessages());
        }
        if (settingsRequest.getPrimaryColor() != null) {
            settings.setPrimaryColor(settingsRequest.getPrimaryColor());
        }
        if (settingsRequest.getBackgroundColor() != null) {
            settings.setBackgroundColor(settingsRequest.getBackgroundColor());
        }
        if (settingsRequest.getFontFamily() != null) {
            settings.setFontFamily(settingsRequest.getFontFamily());
        }
        
        room.setSettings(settings);
        roomService.saveRoom(room);
        
        logger.info("Updated settings for room: {}", roomId);
        ApiResponse<Room> response = ApiResponse.success(room);
        return ResponseEntity.ok(response);
    }
    
    // Inner class for room status response
    public static class RoomStatus {
        private boolean exists;
        private boolean active;
        private int connectedUsers;
        private java.time.LocalDateTime lastActivity;
        
        public RoomStatus(boolean exists, boolean active, int connectedUsers, java.time.LocalDateTime lastActivity) {
            this.exists = exists;
            this.active = active;
            this.connectedUsers = connectedUsers;
            this.lastActivity = lastActivity;
        }
        
        public boolean isExists() { return exists; }
        public void setExists(boolean exists) { this.exists = exists; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public int getConnectedUsers() { return connectedUsers; }
        public void setConnectedUsers(int connectedUsers) { this.connectedUsers = connectedUsers; }
        
        public java.time.LocalDateTime getLastActivity() { return lastActivity; }
        public void setLastActivity(java.time.LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    }
}