package com.timerfy.controller;

import com.timerfy.dto.ApiResponse;
import com.timerfy.dto.CreateMessageRequest;
import com.timerfy.dto.UpdateMessageRequest;
import com.timerfy.exception.RoomNotFoundException;
import com.timerfy.exception.MessageNotFoundException;
import com.timerfy.model.Message;
import com.timerfy.model.MessagePriority;
import com.timerfy.service.RoomService;
import com.timerfy.websocket.MessageEventListener;
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
@RequestMapping("/api/v1/rooms/{roomId}/messages")
@Tag(name = "Message Management", description = "APIs for managing messages within rooms")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class MessageController {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    
    @Autowired
    private RoomService roomService;
    
    @Autowired
    private MessageEventListener messageEventListener;
    
    @PostMapping
    @Operation(
        summary = "Create a new message",
        description = "Creates a new message in the specified room"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Message created successfully",
            content = @Content(schema = @Schema(implementation = Message.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid message data"
        )
    })
    public ResponseEntity<ApiResponse<Message>> createMessage(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Valid @RequestBody CreateMessageRequest request) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        Message message = new Message(request.getText(), request.getPriority(), request.getColor());
        
        if (request.getAutoShow() != null) {
            message.setAutoShow(request.getAutoShow());
        }
        
        if (request.getDuration() != null) {
            message.setDisplayDuration(request.getDuration());
        }
        
        boolean added = roomService.addMessageToRoom(roomId, message);
        
        if (!added) {
            ApiResponse<Message> response = ApiResponse.error("CREATE_FAILED", "Failed to create message");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        // Trigger WebSocket event
        messageEventListener.handleMessageCreated(roomId, message);
        
        logger.info("Created message {} in room {}", message.getId(), roomId);
        ApiResponse<Message> response = ApiResponse.success(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{messageId}")
    @Operation(
        summary = "Update a message",
        description = "Updates message content, visibility, or other properties"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Message updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room or message not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid message data"
        )
    })
    public ResponseEntity<ApiResponse<Message>> updateMessage(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Message ID", required = true)
            @PathVariable String messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        Optional<Message> messageOpt = roomService.getMessageFromRoom(roomId, messageId);
        if (messageOpt.isEmpty()) {
            throw new MessageNotFoundException(roomId, messageId);
        }
        
        Message message = messageOpt.get();
        
        // Update fields if provided
        if (request.getText() != null) {
            message.setText(request.getText());
        }
        if (request.getVisible() != null) {
            message.setVisible(request.getVisible());
        }
        if (request.getPriority() != null) {
            message.setPriority(request.getPriority());
        }
        if (request.getColor() != null) {
            message.setColor(request.getColor());
        }
        
        boolean updated = roomService.updateMessageInRoom(roomId, message);
        
        if (!updated) {
            throw new MessageNotFoundException(roomId, messageId);
        }
        
        logger.info("Updated message {} in room {}", messageId, roomId);
        ApiResponse<Message> response = ApiResponse.success(message);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{messageId}")
    @Operation(
        summary = "Delete a message",
        description = "Permanently removes a message from the room"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Message deleted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room or message not found"
        )
    })
    public ResponseEntity<ApiResponse<String>> deleteMessage(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Message ID", required = true)
            @PathVariable String messageId) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        if (!roomService.getMessageFromRoom(roomId, messageId).isPresent()) {
            throw new MessageNotFoundException(roomId, messageId);
        }
        
        boolean deleted = roomService.removeMessageFromRoom(roomId, messageId);
        
        if (deleted) {
            logger.info("Deleted message {} from room {}", messageId, roomId);
            ApiResponse<String> response = ApiResponse.success("Message deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<String> response = ApiResponse.error("DELETE_FAILED", "Failed to delete message");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/{messageId}/show")
    @Operation(
        summary = "Show a message",
        description = "Makes a hidden message visible"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Message shown successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room or message not found"
        )
    })
    public ResponseEntity<ApiResponse<Message>> showMessage(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Message ID", required = true)
            @PathVariable String messageId) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        Optional<Message> messageOpt = roomService.getMessageFromRoom(roomId, messageId);
        if (messageOpt.isEmpty()) {
            throw new MessageNotFoundException(roomId, messageId);
        }
        
        Message message = messageOpt.get();
        message.show();
        
        boolean updated = roomService.updateMessageInRoom(roomId, message);
        
        if (!updated) {
            throw new MessageNotFoundException(roomId, messageId);
        }
        
        logger.info("Showed message {} in room {}", messageId, roomId);
        ApiResponse<Message> response = ApiResponse.success(message);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{messageId}/hide")
    @Operation(
        summary = "Hide a message",
        description = "Makes a visible message hidden"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Message hidden successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room or message not found"
        )
    })
    public ResponseEntity<ApiResponse<Message>> hideMessage(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Message ID", required = true)
            @PathVariable String messageId) {
        
        if (!roomService.roomExists(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        
        Optional<Message> messageOpt = roomService.getMessageFromRoom(roomId, messageId);
        if (messageOpt.isEmpty()) {
            throw new MessageNotFoundException(roomId, messageId);
        }
        
        Message message = messageOpt.get();
        message.hide();
        
        boolean updated = roomService.updateMessageInRoom(roomId, message);
        
        if (!updated) {
            throw new MessageNotFoundException(roomId, messageId);
        }
        
        logger.info("Hid message {} in room {}", messageId, roomId);
        ApiResponse<Message> response = ApiResponse.success(message);
        return ResponseEntity.ok(response);
    }
}