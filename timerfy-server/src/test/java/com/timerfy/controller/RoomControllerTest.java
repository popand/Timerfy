package com.timerfy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timerfy.dto.RoomSettingsRequest;
import com.timerfy.model.Room;
import com.timerfy.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    private Room testRoom;
    private final String TEST_ROOM_ID = "ABC123";

    @BeforeEach
    void setUp() {
        testRoom = new Room(TEST_ROOM_ID);
        testRoom.setCreated(LocalDateTime.now());
        testRoom.setLastActivity(LocalDateTime.now());
    }

    @Test
    void createRoom_ShouldReturnCreatedRoom() throws Exception {
        // Given
        when(roomService.createRoom()).thenReturn(testRoom);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_ROOM_ID))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.lastActivity").exists());

        verify(roomService).createRoom();
    }

    @Test
    void createRoom_ShouldHandleServiceException() throws Exception {
        // Given
        when(roomService.createRoom()).thenThrow(new RuntimeException("Room creation failed"));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(roomService).createRoom();
    }

    @Test
    void getRoomById_ShouldReturnRoom() throws Exception {
        // Given
        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.of(testRoom));

        // When & Then
        mockMvc.perform(get("/api/v1/rooms/{roomId}", TEST_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_ROOM_ID))
                .andExpect(jsonPath("$.data.createdAt").exists());

        verify(roomService).getRoomById(TEST_ROOM_ID);
        verify(roomService).touchRoom(TEST_ROOM_ID);
    }

    @Test
    void getRoomById_ShouldReturn404WhenNotFound() throws Exception {
        // Given
        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/rooms/{roomId}", TEST_ROOM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).getRoomById(TEST_ROOM_ID);
        verify(roomService, never()).touchRoom(anyString());
    }

    @Test
    void getRoomStatus_ShouldReturnRoomStatus() throws Exception {
        // Given
        testRoom.getStats().setConnectedUsers(5);
        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.of(testRoom));

        // When & Then
        mockMvc.perform(get("/api/v1/rooms/{roomId}/status", TEST_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exists").value(true))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.connectedUsers").value(5))
                .andExpect(jsonPath("$.data.lastActivity").exists());

        verify(roomService).getRoomById(TEST_ROOM_ID);
    }

    @Test
    void getRoomStatus_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/rooms/{roomId}/status", TEST_ROOM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).getRoomById(TEST_ROOM_ID);
    }

    @Test
    void deleteRoom_ShouldDeleteRoom() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.deleteRoom(TEST_ROOM_ID)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/v1/rooms/{roomId}", TEST_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Room deleted successfully"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).deleteRoom(TEST_ROOM_ID);
    }

    @Test
    void deleteRoom_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/v1/rooms/{roomId}", TEST_ROOM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService, never()).deleteRoom(anyString());
    }

    @Test
    void deleteRoom_ShouldReturn500WhenDeletionFails() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.deleteRoom(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/v1/rooms/{roomId}", TEST_ROOM_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DELETE_FAILED"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(roomService).deleteRoom(TEST_ROOM_ID);
    }

    @Test
    void updateRoomSettings_ShouldUpdateSettings() throws Exception {
        // Given
        RoomSettingsRequest settingsRequest = new RoomSettingsRequest();
        settingsRequest.setMaxTimers(15);
        settingsRequest.setAllowViewerMessages(true);
        settingsRequest.setPrimaryColor("#FF0000");
        settingsRequest.setBackgroundColor("#FFFFFF");
        settingsRequest.setFontFamily("Arial");

        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.of(testRoom));

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/settings", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settingsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_ROOM_ID))
                .andExpect(jsonPath("$.data.settings.maxTimers").value(15))
                .andExpect(jsonPath("$.data.settings.allowViewerMessages").value(true))
                .andExpect(jsonPath("$.data.settings.primaryColor").value("#FF0000"))
                .andExpect(jsonPath("$.data.settings.backgroundColor").value("#FFFFFF"))
                .andExpect(jsonPath("$.data.settings.fontFamily").value("Arial"));

        verify(roomService).getRoomById(TEST_ROOM_ID);
        verify(roomService).saveRoom(any(Room.class));
    }

    @Test
    void updateRoomSettings_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        RoomSettingsRequest settingsRequest = new RoomSettingsRequest();
        settingsRequest.setMaxTimers(15);

        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/settings", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settingsRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).getRoomById(TEST_ROOM_ID);
        verify(roomService, never()).saveRoom(any(Room.class));
    }

    @Test
    void updateRoomSettings_ShouldHandlePartialUpdate() throws Exception {
        // Given
        RoomSettingsRequest settingsRequest = new RoomSettingsRequest();
        settingsRequest.setMaxTimers(20); // Only update max timers

        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.of(testRoom));

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/settings", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settingsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settings.maxTimers").value(20));

        verify(roomService).getRoomById(TEST_ROOM_ID);
        verify(roomService).saveRoom(any(Room.class));
    }

    @Test
    void updateRoomSettings_ShouldValidateInvalidJson() throws Exception {
        // Given
        String invalidJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/settings", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(roomService, never()).getRoomById(anyString());
        verify(roomService, never()).saveRoom(any(Room.class));
    }

    @Test
    void updateRoomSettings_ShouldHandleValidationErrors() throws Exception {
        // Given
        RoomSettingsRequest settingsRequest = new RoomSettingsRequest();
        settingsRequest.setMaxTimers(-1); // Invalid negative value

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/settings", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settingsRequest)))
                .andExpect(status().isBadRequest());

        verify(roomService, never()).getRoomById(anyString());
        verify(roomService, never()).saveRoom(any(Room.class));
    }

    @Test
    void corsHeaders_ShouldBePresent() throws Exception {
        // Given
        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.of(testRoom));

        // When & Then
        mockMvc.perform(get("/api/v1/rooms/{roomId}", TEST_ROOM_ID)
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void apiEndpoints_ShouldHaveCorrectContentType() throws Exception {
        // Given
        when(roomService.createRoom()).thenReturn(testRoom);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}