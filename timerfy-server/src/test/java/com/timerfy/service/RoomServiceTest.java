package com.timerfy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timerfy.model.Room;
import com.timerfy.model.Timer;
import com.timerfy.model.Message;
import com.timerfy.model.TimerType;
import com.timerfy.model.MessagePriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RoomIdGenerator roomIdGenerator;

    @InjectMocks
    private RoomService roomService;

    private Room testRoom;
    private Timer testTimer;
    private Message testMessage;
    private final String TEST_ROOM_ID = "ABC123";
    private final String TEST_TIMER_ID = "timer-1";
    private final String TEST_MESSAGE_ID = "message-1";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        // Set up test data
        testRoom = new Room(TEST_ROOM_ID);
        testTimer = new Timer("Test Timer", 60000, TimerType.COUNTDOWN);
        testTimer.setId(TEST_TIMER_ID);
        testMessage = new Message("Test message", MessagePriority.NORMAL, "#FF0000");
        testMessage.setId(TEST_MESSAGE_ID);

        // Set default property values
        ReflectionTestUtils.setField(roomService, "roomExpirationSeconds", 86400L);
        ReflectionTestUtils.setField(roomService, "maxTimersPerRoom", 10);
        ReflectionTestUtils.setField(roomService, "maxUsersPerRoom", 50);
    }

    @Test
    void createRoom_ShouldCreateRoomWithUniqueId() throws Exception {
        // Given
        when(roomIdGenerator.generateRoomId()).thenReturn(TEST_ROOM_ID);
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn(null);
        when(objectMapper.writeValueAsString(any(Room.class))).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");

        // When
        Room createdRoom = roomService.createRoom();

        // Then
        assertNotNull(createdRoom);
        assertEquals(TEST_ROOM_ID, createdRoom.getId());
        assertEquals(10, createdRoom.getSettings().getMaxTimers());
        assertNotNull(createdRoom.getExpiresAt());
        verify(valueOperations).set(eq("room:" + TEST_ROOM_ID), anyString(), eq(86400L), eq(TimeUnit.SECONDS));
        verify(setOperations).add("rooms:active", TEST_ROOM_ID);
    }

    @Test
    void createRoom_ShouldRetryOnDuplicateId() throws Exception {
        // Given
        String duplicateId = "DUP123";
        when(roomIdGenerator.generateRoomId()).thenReturn(duplicateId).thenReturn(TEST_ROOM_ID);
        when(roomIdGenerator.isValidRoomId(anyString())).thenReturn(true);
        when(valueOperations.get("room:" + duplicateId)).thenReturn("{\"id\":\"" + duplicateId + "\"}");
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn(null);
        when(objectMapper.writeValueAsString(any(Room.class))).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");

        // When
        Room createdRoom = roomService.createRoom();

        // Then
        assertEquals(TEST_ROOM_ID, createdRoom.getId());
        verify(roomIdGenerator, times(2)).generateRoomId();
    }

    @Test
    void createRoom_ShouldThrowExceptionAfterMaxAttempts() {
        // Given
        when(roomIdGenerator.generateRoomId()).thenReturn(TEST_ROOM_ID);
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");

        // When & Then
        assertThrows(RuntimeException.class, () -> roomService.createRoom());
        verify(roomIdGenerator, times(10)).generateRoomId();
    }

    @Test
    void getRoomById_ShouldReturnRoomWhenExists() throws Exception {
        // Given
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);

        // When
        Optional<Room> result = roomService.getRoomById(TEST_ROOM_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(TEST_ROOM_ID, result.get().getId());
        verify(valueOperations).get("room:" + TEST_ROOM_ID);
    }

    @Test
    void getRoomById_ShouldReturnEmptyWhenNotExists() {
        // Given
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn(null);

        // When
        Optional<Room> result = roomService.getRoomById(TEST_ROOM_ID);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void getRoomById_ShouldReturnEmptyForInvalidId() {
        // Given
        when(roomIdGenerator.isValidRoomId("INVALID")).thenReturn(false);

        // When
        Optional<Room> result = roomService.getRoomById("INVALID");

        // Then
        assertFalse(result.isPresent());
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void getRoomById_ShouldDeleteExpiredRoom() throws Exception {
        // Given
        Room expiredRoom = new Room(TEST_ROOM_ID);
        expiredRoom.setExpiresAt(LocalDateTime.now().minusHours(1));
        
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(expiredRoom);
        when(redisTemplate.delete("room:" + TEST_ROOM_ID)).thenReturn(true);

        // When
        Optional<Room> result = roomService.getRoomById(TEST_ROOM_ID);

        // Then
        assertFalse(result.isPresent());
        verify(redisTemplate).delete("room:" + TEST_ROOM_ID);
        verify(redisTemplate).delete("room:stats:" + TEST_ROOM_ID);
        verify(setOperations).remove("rooms:active", TEST_ROOM_ID);
    }

    @Test
    void roomExists_ShouldReturnTrueWhenRoomExists() throws Exception {
        // Given
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);

        // When
        boolean exists = roomService.roomExists(TEST_ROOM_ID);

        // Then
        assertTrue(exists);
    }

    @Test
    void roomExists_ShouldReturnFalseWhenRoomNotExists() {
        // Given
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn(null);

        // When
        boolean exists = roomService.roomExists(TEST_ROOM_ID);

        // Then
        assertFalse(exists);
    }

    @Test
    void saveRoom_ShouldSerializeAndSaveRoom() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(testRoom)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");

        // When
        roomService.saveRoom(testRoom);

        // Then
        verify(objectMapper).writeValueAsString(testRoom);
        verify(valueOperations).set("room:" + TEST_ROOM_ID, "{\"id\":\"" + TEST_ROOM_ID + "\"}", 86400L, TimeUnit.SECONDS);
    }

    @Test
    void saveRoom_ShouldThrowExceptionOnSerializationError() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(testRoom)).thenThrow(new RuntimeException("Serialization error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> roomService.saveRoom(testRoom));
    }

    @Test
    void deleteRoom_ShouldDeleteRoomAndCleanup() {
        // Given
        when(redisTemplate.delete("room:" + TEST_ROOM_ID)).thenReturn(true);

        // When
        boolean result = roomService.deleteRoom(TEST_ROOM_ID);

        // Then
        assertTrue(result);
        verify(redisTemplate).delete("room:" + TEST_ROOM_ID);
        verify(redisTemplate).delete("room:stats:" + TEST_ROOM_ID);
        verify(setOperations).remove("rooms:active", TEST_ROOM_ID);
    }

    @Test
    void deleteRoom_ShouldReturnFalseWhenNotExists() {
        // Given
        when(redisTemplate.delete("room:" + TEST_ROOM_ID)).thenReturn(false);

        // When
        boolean result = roomService.deleteRoom(TEST_ROOM_ID);

        // Then
        assertFalse(result);
    }

    @Test
    void addTimerToRoom_ShouldAddTimerWhenRoomExists() throws Exception {
        // Given
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);
        when(objectMapper.writeValueAsString(any(Room.class))).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");

        // When
        boolean result = roomService.addTimerToRoom(TEST_ROOM_ID, testTimer);

        // Then
        assertTrue(result);
        verify(valueOperations).set(eq("room:" + TEST_ROOM_ID), anyString(), eq(86400L), eq(TimeUnit.SECONDS));
    }

    @Test
    void addTimerToRoom_ShouldReturnFalseWhenRoomNotExists() {
        // Given
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn(null);

        // When
        boolean result = roomService.addTimerToRoom(TEST_ROOM_ID, testTimer);

        // Then
        assertFalse(result);
    }

    @Test
    void removeTimerFromRoom_ShouldRemoveTimerWhenExists() throws Exception {
        // Given
        testRoom.addTimer(testTimer);
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);
        when(objectMapper.writeValueAsString(any(Room.class))).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");

        // When
        boolean result = roomService.removeTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID);

        // Then
        assertTrue(result);
        assertNull(testRoom.getTimer(TEST_TIMER_ID));
    }

    @Test
    void getTimerFromRoom_ShouldReturnTimerWhenExists() throws Exception {
        // Given
        testRoom.addTimer(testTimer);
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);

        // When
        Optional<Timer> result = roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(TEST_TIMER_ID, result.get().getId());
    }

    @Test
    void addMessageToRoom_ShouldAddMessageWhenRoomExists() throws Exception {
        // Given
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);
        when(objectMapper.writeValueAsString(any(Room.class))).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");

        // When
        boolean result = roomService.addMessageToRoom(TEST_ROOM_ID, testMessage);

        // Then
        assertTrue(result);
        verify(valueOperations).set(eq("room:" + TEST_ROOM_ID), anyString(), eq(86400L), eq(TimeUnit.SECONDS));
    }

    @Test
    void updateRoomStats_ShouldUpdateStatsWhenRoomExists() throws Exception {
        // Given
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);
        when(objectMapper.writeValueAsString(any(Room.class))).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");

        // When
        roomService.updateRoomStats(TEST_ROOM_ID, 5, 2, 3);

        // Then
        assertEquals(5, testRoom.getStats().getConnectedUsers());
        assertEquals(2, testRoom.getStats().getTotalControllers());
        assertEquals(3, testRoom.getStats().getTotalViewers());
        verify(valueOperations).set(eq("room:" + TEST_ROOM_ID), anyString(), eq(86400L), eq(TimeUnit.SECONDS));
    }

    @Test
    void touchRoom_ShouldUpdateLastActivity() throws Exception {
        // Given
        LocalDateTime beforeTouch = testRoom.getLastActivity();
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);
        when(objectMapper.writeValueAsString(any(Room.class))).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");

        // When
        roomService.touchRoom(TEST_ROOM_ID);

        // Then
        assertTrue(testRoom.getLastActivity().isAfter(beforeTouch));
        verify(valueOperations).set(eq("room:" + TEST_ROOM_ID), anyString(), eq(86400L), eq(TimeUnit.SECONDS));
    }

    @Test
    void getActiveRoomsCount_ShouldReturnCount() {
        // Given
        when(setOperations.size("rooms:active")).thenReturn(5L);

        // When
        long count = roomService.getActiveRoomsCount();

        // Then
        assertEquals(5L, count);
    }

    @Test
    void getActiveRoomsCount_ShouldReturnZeroWhenNull() {
        // Given
        when(setOperations.size("rooms:active")).thenReturn(null);

        // When
        long count = roomService.getActiveRoomsCount();

        // Then
        assertEquals(0L, count);
    }

    @Test
    void isRoomAtCapacity_ShouldReturnTrueWhenAtCapacity() throws Exception {
        // Given
        testRoom.getStats().setConnectedUsers(50);
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);

        // When
        boolean atCapacity = roomService.isRoomAtCapacity(TEST_ROOM_ID);

        // Then
        assertTrue(atCapacity);
    }

    @Test
    void isRoomAtCapacity_ShouldReturnFalseWhenNotAtCapacity() throws Exception {
        // Given
        testRoom.getStats().setConnectedUsers(10);
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);

        // When
        boolean atCapacity = roomService.isRoomAtCapacity(TEST_ROOM_ID);

        // Then
        assertFalse(atCapacity);
    }

    @Test
    void canAddTimer_ShouldReturnTrueWhenUnderLimit() throws Exception {
        // Given
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);

        // When
        boolean canAdd = roomService.canAddTimer(TEST_ROOM_ID);

        // Then
        assertTrue(canAdd);
    }

    @Test
    void canAddTimer_ShouldReturnFalseWhenAtLimit() throws Exception {
        // Given
        testRoom.getSettings().setMaxTimers(1);
        testRoom.addTimer(testTimer);
        when(roomIdGenerator.isValidRoomId(TEST_ROOM_ID)).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn("{\"id\":\"" + TEST_ROOM_ID + "\"}");
        when(objectMapper.readValue(anyString(), eq(Room.class))).thenReturn(testRoom);

        // When
        boolean canAdd = roomService.canAddTimer(TEST_ROOM_ID);

        // Then
        assertFalse(canAdd);
    }

    @Test
    void cleanupExpiredRooms_ShouldRemoveExpiredRooms() {
        // Given
        when(setOperations.members("rooms:active")).thenReturn(Set.of(TEST_ROOM_ID, "ROOM2"));
        when(roomIdGenerator.isValidRoomId(anyString())).thenReturn(true);
        when(valueOperations.get("room:" + TEST_ROOM_ID)).thenReturn(null);
        when(valueOperations.get("room:ROOM2")).thenReturn(null);

        // When
        roomService.cleanupExpiredRooms();

        // Then
        verify(setOperations).remove("rooms:active", TEST_ROOM_ID);
        verify(setOperations).remove("rooms:active", "ROOM2");
    }
}