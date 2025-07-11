package com.timerfy.websocket;

import com.timerfy.model.Timer;
import com.timerfy.service.TimerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TimerEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(TimerEventListener.class);
    private static final int MAX_EVENTS_PER_SECOND = 10;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // Rate limiting for timer events
    private final Map<String, AtomicLong> lastEventTime = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventCount = new ConcurrentHashMap<>();
    
    @Async
    @EventListener
    public void handleTimerEvent(TimerService.TimerEvent event) {
        String roomId = event.getRoomId();
        String eventType = event.getEventType();
        Timer timer = event.getTimer();
        
        // Apply rate limiting for TIMER_TICK events
        if ("TIMER_TICK".equals(eventType) && shouldRateLimit(roomId)) {
            return;
        }
        
        try {
            switch (eventType) {
                case "TIMER_CREATED":
                    handleTimerCreated(roomId, timer);
                    break;
                case "TIMER_UPDATED":
                    handleTimerUpdated(roomId, timer);
                    break;
                case "TIMER_DELETED":
                    handleTimerDeleted(roomId, timer);
                    break;
                case "TIMER_STARTED":
                    handleTimerStarted(roomId, timer);
                    break;
                case "TIMER_STOPPED":
                    handleTimerStopped(roomId, timer);
                    break;
                case "TIMER_PAUSED":
                    handleTimerPaused(roomId, timer);
                    break;
                case "TIMER_RESET":
                    handleTimerReset(roomId, timer);
                    break;
                case "TIMER_ADJUSTED":
                    handleTimerAdjusted(roomId, timer);
                    break;
                case "TIMER_TICK":
                    handleTimerTick(roomId, timer);
                    break;
                case "TIMER_WARNING":
                    handleTimerWarning(roomId, timer);
                    break;
                case "TIMER_CRITICAL":
                    handleTimerCritical(roomId, timer);
                    break;
                case "TIMER_COMPLETED":
                    handleTimerCompleted(roomId, timer);
                    break;
                default:
                    logger.warn("Unknown timer event type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("Error handling timer event {} for timer {} in room {}: {}", 
                        eventType, timer.getId(), roomId, e.getMessage());
        }
    }
    
    private void handleTimerCreated(String roomId, Timer timer) {
        TimerEventDto eventDto = new TimerEventDto(
            timer.getId(),
            timer.getName(),
            timer.getDuration(),
            timer.getCurrentTime(),
            timer.getState(),
            timer.getType(),
            timer.getCreatedAt(),
            timer.getStartedAt(),
            null,
            timer.getSettings()
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_CREATED", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.debug("Broadcasted timer created event for timer {} in room {}", timer.getId(), roomId);
    }
    
    private void handleTimerUpdated(String roomId, Timer timer) {
        TimerEventDto eventDto = new TimerEventDto(
            timer.getId(),
            timer.getName(),
            timer.getDuration(),
            timer.getCurrentTime(),
            timer.getState(),
            timer.getType(),
            timer.getCreatedAt(),
            timer.getStartedAt(),
            null,
            timer.getSettings()
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_UPDATED", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.debug("Broadcasted timer updated event for timer {} in room {}", timer.getId(), roomId);
    }
    
    private void handleTimerDeleted(String roomId, Timer timer) {
        TimerDeletedEventDto eventDto = new TimerDeletedEventDto(timer.getId(), timer.getName());
        
        WebSocketMessage message = new WebSocketMessage("TIMER_DELETED", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.debug("Broadcasted timer deleted event for timer {} in room {}", timer.getId(), roomId);
    }
    
    private void handleTimerStarted(String roomId, Timer timer) {
        TimerControlEventDto eventDto = new TimerControlEventDto(
            timer.getId(),
            timer.getState(),
            timer.getCurrentTime(),
            timer.getStartedAt()
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_STARTED", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.debug("Broadcasted timer started event for timer {} in room {}", timer.getId(), roomId);
    }
    
    private void handleTimerStopped(String roomId, Timer timer) {
        TimerControlEventDto eventDto = new TimerControlEventDto(
            timer.getId(),
            timer.getState(),
            timer.getCurrentTime(),
            null
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_STOPPED", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.debug("Broadcasted timer stopped event for timer {} in room {}", timer.getId(), roomId);
    }
    
    private void handleTimerPaused(String roomId, Timer timer) {
        TimerControlEventDto eventDto = new TimerControlEventDto(
            timer.getId(),
            timer.getState(),
            timer.getCurrentTime(),
            timer.getPausedAt()
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_PAUSED", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.debug("Broadcasted timer paused event for timer {} in room {}", timer.getId(), roomId);
    }
    
    private void handleTimerReset(String roomId, Timer timer) {
        TimerControlEventDto eventDto = new TimerControlEventDto(
            timer.getId(),
            timer.getState(),
            timer.getCurrentTime(),
            null
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_RESET", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.debug("Broadcasted timer reset event for timer {} in room {}", timer.getId(), roomId);
    }
    
    private void handleTimerAdjusted(String roomId, Timer timer) {
        TimerControlEventDto eventDto = new TimerControlEventDto(
            timer.getId(),
            timer.getState(),
            timer.getCurrentTime(),
            timer.getStartedAt()
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_ADJUSTED", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.debug("Broadcasted timer adjusted event for timer {} in room {}", timer.getId(), roomId);
    }
    
    private void handleTimerTick(String roomId, Timer timer) {
        // Only send essential data for tick events to minimize bandwidth
        TimerTickEventDto eventDto = new TimerTickEventDto(
            timer.getId(),
            timer.getCurrentTime(),
            timer.getState()
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_UPDATED", eventDto);
        broadcastToRoom(roomId, message);
        
        // Don't log every tick to avoid log spam
    }
    
    private void handleTimerWarning(String roomId, Timer timer) {
        TimerAlertEventDto eventDto = new TimerAlertEventDto(
            timer.getId(),
            "WARNING",
            timer.getCurrentTime(),
            timer.getSettings().getWarningTime(),
            "Timer entering warning state"
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_WARNING", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.info("Broadcasted timer warning for timer {} in room {}, time remaining: {}", 
                   timer.getId(), roomId, timer.getCurrentTime());
    }
    
    private void handleTimerCritical(String roomId, Timer timer) {
        TimerAlertEventDto eventDto = new TimerAlertEventDto(
            timer.getId(),
            "CRITICAL",
            timer.getCurrentTime(),
            timer.getSettings().getCriticalTime(),
            "Timer entering critical state"
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_CRITICAL", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.info("Broadcasted timer critical alert for timer {} in room {}, time remaining: {}", 
                   timer.getId(), roomId, timer.getCurrentTime());
    }
    
    private void handleTimerCompleted(String roomId, Timer timer) {
        TimerCompletedEventDto eventDto = new TimerCompletedEventDto(
            timer.getId(),
            timer.getName(),
            timer.getCompletedAt(),
            timer.getSettings().getPlaySound(),
            timer.getSettings().getAutoReset()
        );
        
        WebSocketMessage message = new WebSocketMessage("TIMER_COMPLETED", eventDto);
        broadcastToRoom(roomId, message);
        
        logger.info("Broadcasted timer completed event for timer {} in room {}", timer.getId(), roomId);
    }
    
    private boolean shouldRateLimit(String roomId) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastEventTime.computeIfAbsent(roomId, k -> new AtomicLong(0)).get();
        
        // Reset counter if more than 1 second has passed
        if (currentTime - lastTime > 1000) {
            lastEventTime.get(roomId).set(currentTime);
            eventCount.computeIfAbsent(roomId, k -> new AtomicLong(0)).set(0);
            return false;
        }
        
        // Check if we've exceeded the rate limit
        long count = eventCount.get(roomId).incrementAndGet();
        return count > MAX_EVENTS_PER_SECOND;
    }
    
    private void broadcastToRoom(String roomId, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
        } catch (Exception e) {
            logger.error("Failed to broadcast message to room {}: {}", roomId, e.getMessage());
        }
    }
}