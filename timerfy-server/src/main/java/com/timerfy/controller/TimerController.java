package com.timerfy.controller;

import com.timerfy.dto.ApiResponse;
import com.timerfy.dto.CreateTimerRequest;
import com.timerfy.dto.TimerControlRequest;
import com.timerfy.dto.UpdateTimerRequest;
import com.timerfy.exception.RoomNotFoundException;
import com.timerfy.exception.TimerNotFoundException;
import com.timerfy.exception.TimerLimitExceededException;
import com.timerfy.exception.InvalidTimerStateException;
import com.timerfy.model.Timer;
import com.timerfy.service.RoomService;
import com.timerfy.service.TimerService;
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
@RequestMapping("/api/v1/rooms/{roomId}/timers")
@Tag(name = "Timer Management", description = "APIs for managing timers within rooms")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TimerController {
    
    private static final Logger logger = LoggerFactory.getLogger(TimerController.class);
    
    @Autowired
    private TimerService timerService;
    
    @Autowired
    private RoomService roomService;
    
    @PostMapping
    @Operation(
        summary = "Create a new timer",
        description = "Creates a new timer in the specified room"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Timer created successfully",
            content = @Content(schema = @Schema(implementation = Timer.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Timer limit exceeded"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid timer data"
        )
    })
    public ResponseEntity<ApiResponse<Timer>> createTimer(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Valid @RequestBody CreateTimerRequest request) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        if (!roomService.canAddTimer(roomId)) {
            throw new TimerLimitExceededException(roomId);
        }
        
        try {
            Timer timer = timerService.createTimer(
                roomId,
                request.getName(),
                request.getDuration(),
                request.getType()
            );
            
            // Apply custom settings if provided
            if (request.getSettings() != null) {
                Timer.TimerSettings settings = new Timer.TimerSettings();
                settings.setWarningTime(request.getSettings().getWarningTime());
                settings.setCriticalTime(request.getSettings().getCriticalTime());
                settings.setAutoReset(request.getSettings().getAutoReset());
                settings.setPlaySound(request.getSettings().getPlaySound());
                settings.setShowNotifications(request.getSettings().getShowNotifications());
                timer.setSettings(settings);
                roomService.updateTimerInRoom(roomId, timer);
            }
            
            logger.info("Created timer {} in room {}", timer.getId(), roomId);
            ApiResponse<Timer> response = ApiResponse.success(timer);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalStateException e) {
            throw new TimerLimitExceededException(roomId);
        }
    }
    
    @PutMapping("/{timerId}")
    @Operation(
        summary = "Update timer configuration",
        description = "Updates timer properties like name, duration, and settings"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Timer updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room or timer not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid timer data"
        )
    })
    public ResponseEntity<ApiResponse<Timer>> updateTimer(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Timer ID", required = true)
            @PathVariable String timerId,
            @Valid @RequestBody UpdateTimerRequest request) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        Optional<Timer> timerOpt = roomService.getTimerFromRoom(roomId, timerId);
        if (timerOpt.isEmpty()) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        Timer.TimerSettings settings = null;
        if (request.getSettings() != null) {
            settings = new Timer.TimerSettings();
            settings.setWarningTime(request.getSettings().getWarningTime());
            settings.setCriticalTime(request.getSettings().getCriticalTime());
            settings.setAutoReset(request.getSettings().getAutoReset());
            settings.setPlaySound(request.getSettings().getPlaySound());
            settings.setShowNotifications(request.getSettings().getShowNotifications());
        }
        
        boolean updated = timerService.updateTimer(
            roomId,
            timerId,
            request.getName(),
            request.getDuration(),
            settings
        );
        
        if (!updated) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        Timer updatedTimer = roomService.getTimerFromRoom(roomId, timerId).get();
        logger.info("Updated timer {} in room {}", timerId, roomId);
        
        ApiResponse<Timer> response = ApiResponse.success(updatedTimer);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{timerId}")
    @Operation(
        summary = "Delete a timer",
        description = "Permanently removes a timer from the room"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Timer deleted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room or timer not found"
        )
    })
    public ResponseEntity<ApiResponse<String>> deleteTimer(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Timer ID", required = true)
            @PathVariable String timerId) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        if (!roomService.getTimerFromRoom(roomId, timerId).isPresent()) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        boolean deleted = timerService.deleteTimer(roomId, timerId);
        
        if (deleted) {
            logger.info("Deleted timer {} from room {}", timerId, roomId);
            ApiResponse<String> response = ApiResponse.success("Timer deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<String> response = ApiResponse.error("DELETE_FAILED", "Failed to delete timer");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/{timerId}/start")
    @Operation(
        summary = "Start a timer",
        description = "Starts the specified timer"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Timer started successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room or timer not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Timer cannot be started in current state"
        )
    })
    public ResponseEntity<ApiResponse<Timer>> startTimer(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Timer ID", required = true)
            @PathVariable String timerId,
            @RequestBody(required = false) TimerControlRequest request) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        Optional<Timer> timerOpt = roomService.getTimerFromRoom(roomId, timerId);
        if (timerOpt.isEmpty()) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        boolean started = timerService.startTimer(
            roomId,
            timerId,
            request != null ? request.getStartTime() : null
        );
        
        if (!started) {
            Timer timer = timerOpt.get();
            throw new InvalidTimerStateException(timerId, timer.getState().toString(), "start");
        }
        
        Timer updatedTimer = roomService.getTimerFromRoom(roomId, timerId).get();
        logger.info("Started timer {} in room {}", timerId, roomId);
        
        ApiResponse<Timer> response = ApiResponse.success(updatedTimer);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{timerId}/stop")
    @Operation(
        summary = "Stop a timer",
        description = "Stops the specified timer and resets it to initial state"
    )
    public ResponseEntity<ApiResponse<Timer>> stopTimer(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Timer ID", required = true)
            @PathVariable String timerId) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        if (!roomService.getTimerFromRoom(roomId, timerId).isPresent()) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        boolean stopped = timerService.stopTimer(roomId, timerId);
        
        if (!stopped) {
            Timer timer = roomService.getTimerFromRoom(roomId, timerId).get();
            throw new InvalidTimerStateException(timerId, timer.getState().toString(), "stop");
        }
        
        Timer updatedTimer = roomService.getTimerFromRoom(roomId, timerId).get();
        logger.info("Stopped timer {} in room {}", timerId, roomId);
        
        ApiResponse<Timer> response = ApiResponse.success(updatedTimer);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{timerId}/pause")
    @Operation(
        summary = "Pause a timer",
        description = "Pauses the specified timer, preserving current state"
    )
    public ResponseEntity<ApiResponse<Timer>> pauseTimer(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Timer ID", required = true)
            @PathVariable String timerId) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        if (!roomService.getTimerFromRoom(roomId, timerId).isPresent()) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        boolean paused = timerService.pauseTimer(roomId, timerId);
        
        if (!paused) {
            Timer timer = roomService.getTimerFromRoom(roomId, timerId).get();
            throw new InvalidTimerStateException(timerId, timer.getState().toString(), "pause");
        }
        
        Timer updatedTimer = roomService.getTimerFromRoom(roomId, timerId).get();
        logger.info("Paused timer {} in room {}", timerId, roomId);
        
        ApiResponse<Timer> response = ApiResponse.success(updatedTimer);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{timerId}/reset")
    @Operation(
        summary = "Reset a timer",
        description = "Resets timer to initial duration or sets new duration"
    )
    public ResponseEntity<ApiResponse<Timer>> resetTimer(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Timer ID", required = true)
            @PathVariable String timerId,
            @RequestBody(required = false) TimerControlRequest request) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        if (!roomService.getTimerFromRoom(roomId, timerId).isPresent()) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        boolean reset = timerService.resetTimer(
            roomId,
            timerId,
            request != null ? request.getNewDuration() : null
        );
        
        if (!reset) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        Timer updatedTimer = roomService.getTimerFromRoom(roomId, timerId).get();
        logger.info("Reset timer {} in room {}", timerId, roomId);
        
        ApiResponse<Timer> response = ApiResponse.success(updatedTimer);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{timerId}/adjust")
    @Operation(
        summary = "Adjust timer time",
        description = "Adds or subtracts time from the current timer value"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Timer adjusted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room or timer not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid adjustment value"
        )
    })
    public ResponseEntity<ApiResponse<Timer>> adjustTimer(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Timer ID", required = true)
            @PathVariable String timerId,
            @Valid @RequestBody TimerControlRequest request) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        if (!roomService.getTimerFromRoom(roomId, timerId).isPresent()) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        if (request.getAdjustment() == null) {
            throw new IllegalArgumentException("Adjustment value is required");
        }
        
        boolean adjusted = timerService.adjustTimer(roomId, timerId, request.getAdjustment());
        
        if (!adjusted) {
            throw new TimerNotFoundException(roomId, timerId);
        }
        
        Timer updatedTimer = roomService.getTimerFromRoom(roomId, timerId).get();
        logger.info("Adjusted timer {} in room {} by {} seconds", timerId, roomId, request.getAdjustment());
        
        ApiResponse<Timer> response = ApiResponse.success(updatedTimer);
        return ResponseEntity.ok(response);
    }
}