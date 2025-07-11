package com.timerfy.websocket;

import com.timerfy.model.Message;
import com.timerfy.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MessageEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageEventListener.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private RoomService roomService;
    
    public void handleMessageCreated(String roomId, Message message) {
        MessageEventDto eventDto = new MessageEventDto(
            message.getId(),
            message.getText(),
            message.getVisible(),
            message.getPriority(),
            message.getColor(),
            message.getTimestamp(),
            message.getAutoHideAt(),
            message.getCreatedBy()
        );
        
        WebSocketMessage wsMessage = new WebSocketMessage("MESSAGE_CREATED", eventDto);
        broadcastToRoom(roomId, wsMessage);
        
        logger.debug("Broadcasted message created event for message {} in room {}", message.getId(), roomId);
    }
    
    public void handleMessageUpdated(String roomId, Message message) {
        MessageEventDto eventDto = new MessageEventDto(
            message.getId(),
            message.getText(),
            message.getVisible(),
            message.getPriority(),
            message.getColor(),
            message.getTimestamp(),
            message.getAutoHideAt(),
            message.getCreatedBy()
        );
        
        WebSocketMessage wsMessage = new WebSocketMessage("MESSAGE_UPDATED", eventDto);
        broadcastToRoom(roomId, wsMessage);
        
        logger.debug("Broadcasted message updated event for message {} in room {}", message.getId(), roomId);
    }
    
    public void handleMessageDeleted(String roomId, String messageId, String messageText) {
        MessageDeletedEventDto eventDto = new MessageDeletedEventDto(messageId, messageText);
        
        WebSocketMessage wsMessage = new WebSocketMessage("MESSAGE_DELETED", eventDto);
        broadcastToRoom(roomId, wsMessage);
        
        logger.debug("Broadcasted message deleted event for message {} in room {}", messageId, roomId);
    }
    
    public void handleMessageVisibilityChanged(String roomId, String messageId, boolean visible) {
        MessageVisibilityEventDto eventDto = new MessageVisibilityEventDto(messageId, visible);
        
        String eventType = visible ? "MESSAGE_SHOWN" : "MESSAGE_HIDDEN";
        WebSocketMessage wsMessage = new WebSocketMessage(eventType, eventDto);
        broadcastToRoom(roomId, wsMessage);
        
        logger.debug("Broadcasted message visibility event for message {} in room {}: {}", 
                    messageId, roomId, visible ? "shown" : "hidden");
    }
    
    @Scheduled(fixedRate = 30000) // Check every 30 seconds
    public void processAutoHideMessages() {
        // This would ideally get all active rooms, but for now we'll handle it differently
        // The auto-hide logic will be triggered when rooms are accessed
        logger.trace("Checking for auto-hide messages...");
    }
    
    public void checkAndHideExpiredMessages(String roomId) {
        try {
            Optional<com.timerfy.model.Room> roomOpt = roomService.getRoomById(roomId);
            if (roomOpt.isEmpty()) {
                return;
            }
            
            com.timerfy.model.Room room = roomOpt.get();
            boolean roomUpdated = false;
            
            for (Message message : room.getMessages()) {
                if (message.getVisible() && message.shouldAutoHide()) {
                    message.hide();
                    roomUpdated = true;
                    
                    // Broadcast the auto-hide event
                    handleMessageVisibilityChanged(roomId, message.getId(), false);
                    
                    logger.info("Auto-hid message {} in room {} after timeout", message.getId(), roomId);
                }
            }
            
            if (roomUpdated) {
                roomService.saveRoom(room);
            }
        } catch (Exception e) {
            logger.error("Error checking auto-hide messages for room {}: {}", roomId, e.getMessage());
        }
    }
    
    private void broadcastToRoom(String roomId, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
        } catch (Exception e) {
            logger.error("Failed to broadcast message to room {}: {}", roomId, e.getMessage());
        }
    }
}