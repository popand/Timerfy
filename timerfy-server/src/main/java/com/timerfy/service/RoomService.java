package com.timerfy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timerfy.model.Room;
import com.timerfy.model.Timer;
import com.timerfy.model.Message;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Validated
public class RoomService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String ROOM_STATS_KEY_PREFIX = "room:stats:";
    private static final String ROOMS_SET_KEY = "rooms:active";
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private RoomIdGenerator roomIdGenerator;
    
    @Value("${timerfy.room.expiration:86400}")
    private long roomExpirationSeconds;
    
    @Value("${timerfy.room.max-timers:10}")
    private int maxTimersPerRoom;
    
    @Value("${timerfy.room.max-users:50}")
    private int maxUsersPerRoom;
    
    public Room createRoom() {
        String roomId = generateUniqueRoomId();
        Room room = new Room(roomId);
        
        room.getSettings().setMaxTimers(maxTimersPerRoom);
        room.setExpiresAt(LocalDateTime.now().plusSeconds(roomExpirationSeconds));
        
        saveRoom(room);
        addToActiveRooms(roomId);
        
        logger.info("Created new room: {}", roomId);
        return room;
    }
    
    public Optional<Room> getRoomById(String roomId) {
        if (!roomIdGenerator.isValidRoomId(roomId)) {
            return Optional.empty();
        }
        
        try {
            String roomJson = redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);
            if (roomJson == null) {
                return Optional.empty();
            }
            
            Room room = objectMapper.readValue(roomJson, Room.class);
            
            if (room.isExpired()) {
                deleteRoom(roomId);
                return Optional.empty();
            }
            
            return Optional.of(room);
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing room {}: {}", roomId, e.getMessage());
            return Optional.empty();
        }
    }
    
    public boolean roomExists(String roomId) {
        return getRoomById(roomId).isPresent();
    }
    
    public void saveRoom(@Valid Room room) {
        try {
            String roomJson = objectMapper.writeValueAsString(room);
            String roomKey = ROOM_KEY_PREFIX + room.getId();
            
            redisTemplate.opsForValue().set(roomKey, roomJson, roomExpirationSeconds, TimeUnit.SECONDS);
            
            room.updateLastActivity();
            logger.debug("Saved room: {}", room.getId());
        } catch (JsonProcessingException e) {
            logger.error("Error serializing room {}: {}", room.getId(), e.getMessage());
            throw new RuntimeException("Failed to save room", e);
        }
    }
    
    public boolean updateRoom(String roomId, Room updatedRoom) {
        Optional<Room> existingRoom = getRoomById(roomId);
        if (existingRoom.isEmpty()) {
            return false;
        }
        
        updatedRoom.setId(roomId);
        updatedRoom.updateLastActivity();
        saveRoom(updatedRoom);
        
        logger.info("Updated room: {}", roomId);
        return true;
    }
    
    public boolean deleteRoom(String roomId) {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        String statsKey = ROOM_STATS_KEY_PREFIX + roomId;
        
        Boolean deleted = redisTemplate.delete(roomKey);
        redisTemplate.delete(statsKey);
        removeFromActiveRooms(roomId);
        
        if (Boolean.TRUE.equals(deleted)) {
            logger.info("Deleted room: {}", roomId);
            return true;
        }
        
        return false;
    }
    
    public boolean addTimerToRoom(String roomId, Timer timer) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }
        
        Room room = roomOpt.get();
        
        try {
            room.addTimer(timer);
            saveRoom(room);
            logger.info("Added timer {} to room {}", timer.getId(), roomId);
            return true;
        } catch (IllegalStateException e) {
            logger.warn("Failed to add timer to room {}: {}", roomId, e.getMessage());
            return false;
        }
    }
    
    public boolean removeTimerFromRoom(String roomId, String timerId) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }
        
        Room room = roomOpt.get();
        room.removeTimer(timerId);
        saveRoom(room);
        
        logger.info("Removed timer {} from room {}", timerId, roomId);
        return true;
    }
    
    public Optional<Timer> getTimerFromRoom(String roomId, String timerId) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(roomOpt.get().getTimer(timerId));
    }
    
    public boolean updateTimerInRoom(String roomId, Timer updatedTimer) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }
        
        Room room = roomOpt.get();
        Timer existingTimer = room.getTimer(updatedTimer.getId());
        
        if (existingTimer == null) {
            return false;
        }
        
        room.removeTimer(updatedTimer.getId());
        room.addTimer(updatedTimer);
        saveRoom(room);
        
        logger.info("Updated timer {} in room {}", updatedTimer.getId(), roomId);
        return true;
    }
    
    public boolean addMessageToRoom(String roomId, Message message) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }
        
        Room room = roomOpt.get();
        room.addMessage(message);
        saveRoom(room);
        
        logger.info("Added message {} to room {}", message.getId(), roomId);
        return true;
    }
    
    public boolean removeMessageFromRoom(String roomId, String messageId) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }
        
        Room room = roomOpt.get();
        room.removeMessage(messageId);
        saveRoom(room);
        
        logger.info("Removed message {} from room {}", messageId, roomId);
        return true;
    }
    
    public Optional<Message> getMessageFromRoom(String roomId, String messageId) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(roomOpt.get().getMessage(messageId));
    }
    
    public boolean updateMessageInRoom(String roomId, Message updatedMessage) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }
        
        Room room = roomOpt.get();
        Message existingMessage = room.getMessage(updatedMessage.getId());
        
        if (existingMessage == null) {
            return false;
        }
        
        room.removeMessage(updatedMessage.getId());
        room.addMessage(updatedMessage);
        saveRoom(room);
        
        logger.info("Updated message {} in room {}", updatedMessage.getId(), roomId);
        return true;
    }
    
    public void updateRoomStats(String roomId, int connectedUsers, int controllers, int viewers) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return;
        }
        
        Room room = roomOpt.get();
        Room.RoomStats stats = room.getStats();
        stats.setConnectedUsers(connectedUsers);
        stats.setTotalControllers(controllers);
        stats.setTotalViewers(viewers);
        
        saveRoom(room);
        
        logger.debug("Updated stats for room {}: users={}, controllers={}, viewers={}", 
                    roomId, connectedUsers, controllers, viewers);
    }
    
    public void touchRoom(String roomId) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            room.updateLastActivity();
            saveRoom(room);
        }
    }
    
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredRooms() {
        Set<String> activeRooms = redisTemplate.opsForSet().members(ROOMS_SET_KEY);
        
        if (activeRooms == null || activeRooms.isEmpty()) {
            return;
        }
        
        int cleanedCount = 0;
        for (String roomId : activeRooms) {
            Optional<Room> roomOpt = getRoomById(roomId);
            if (roomOpt.isEmpty()) {
                removeFromActiveRooms(roomId);
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            logger.info("Cleaned up {} expired rooms", cleanedCount);
        }
    }
    
    private String generateUniqueRoomId() {
        String roomId;
        int attempts = 0;
        final int maxAttempts = 10;
        
        do {
            roomId = roomIdGenerator.generateRoomId();
            attempts++;
        } while (roomExists(roomId) && attempts < maxAttempts);
        
        if (attempts >= maxAttempts) {
            throw new RuntimeException("Failed to generate unique room ID after " + maxAttempts + " attempts");
        }
        
        return roomId;
    }
    
    private void addToActiveRooms(String roomId) {
        redisTemplate.opsForSet().add(ROOMS_SET_KEY, roomId);
    }
    
    private void removeFromActiveRooms(String roomId) {
        redisTemplate.opsForSet().remove(ROOMS_SET_KEY, roomId);
    }
    
    public long getActiveRoomsCount() {
        Long count = redisTemplate.opsForSet().size(ROOMS_SET_KEY);
        return count != null ? count : 0;
    }
    
    public boolean isRoomAtCapacity(String roomId) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return true;
        }
        
        Room room = roomOpt.get();
        return room.getStats().getConnectedUsers() >= maxUsersPerRoom;
    }
    
    public boolean canAddTimer(String roomId) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return false;
        }
        
        Room room = roomOpt.get();
        return room.getTimers().size() < room.getSettings().getMaxTimers();
    }
}