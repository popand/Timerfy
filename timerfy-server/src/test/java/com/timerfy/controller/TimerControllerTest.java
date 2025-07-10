package com.timerfy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timerfy.dto.CreateTimerRequest;
import com.timerfy.dto.TimerControlRequest;
import com.timerfy.dto.UpdateTimerRequest;
import com.timerfy.model.Timer;
import com.timerfy.model.TimerType;
import com.timerfy.service.RoomService;
import com.timerfy.service.TimerService;
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

@WebMvcTest(TimerController.class)
class TimerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @MockBean
    private TimerService timerService;

    private Timer testTimer;
    private final String TEST_ROOM_ID = "ABC123";
    private final String TEST_TIMER_ID = "timer-1";

    @BeforeEach
    void setUp() {
        testTimer = new Timer("Test Timer", 60000L, TimerType.COUNTDOWN);
        testTimer.setId(TEST_TIMER_ID);
        testTimer.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createTimer_ShouldCreateTimerSuccessfully() throws Exception {
        // Given
        CreateTimerRequest request = new CreateTimerRequest();
        request.setName("New Timer");
        request.setDuration(120000L);
        request.setType(TimerType.COUNTDOWN);

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.createTimer(TEST_ROOM_ID, "New Timer", 120000L, TimerType.COUNTDOWN)).thenReturn(testTimer);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_TIMER_ID))
                .andExpect(jsonPath("$.data.name").value("Test Timer"))
                .andExpect(jsonPath("$.data.duration").value(60000))
                .andExpect(jsonPath("$.data.type").value("COUNTDOWN"))
                .andExpect(jsonPath("$.data.state").value("STOPPED"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).createTimer(TEST_ROOM_ID, "New Timer", 120000L, TimerType.COUNTDOWN);
    }

    @Test
    void createTimer_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        CreateTimerRequest request = new CreateTimerRequest();
        request.setName("New Timer");
        request.setDuration(120000L);
        request.setType(TimerType.COUNTDOWN);

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService, never()).createTimer(anyString(), anyString(), anyLong(), any(TimerType.class));
    }

    @Test
    void createTimer_ShouldHandleServiceException() throws Exception {
        // Given
        CreateTimerRequest request = new CreateTimerRequest();
        request.setName("New Timer");
        request.setDuration(120000L);
        request.setType(TimerType.COUNTDOWN);

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.createTimer(TEST_ROOM_ID, "New Timer", 120000L, TimerType.COUNTDOWN))
                .thenThrow(new IllegalStateException("Cannot add more timers"));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Cannot add more timers"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).createTimer(TEST_ROOM_ID, "New Timer", 120000L, TimerType.COUNTDOWN);
    }

    @Test
    void createTimer_ShouldValidateRequiredFields() throws Exception {
        // Given
        CreateTimerRequest request = new CreateTimerRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers", TEST_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(roomService, never()).roomExists(anyString());
        verify(timerService, never()).createTimer(anyString(), anyString(), anyLong(), any(TimerType.class));
    }

    @Test
    void updateTimer_ShouldUpdateTimerSuccessfully() throws Exception {
        // Given
        UpdateTimerRequest request = new UpdateTimerRequest();
        request.setName("Updated Timer");
        request.setDuration(180000L);

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.updateTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), eq("Updated Timer"), eq(180000L), any())).thenReturn(true);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/timers/{timerId}", TEST_ROOM_ID, TEST_TIMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_TIMER_ID));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).updateTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), eq("Updated Timer"), eq(180000L), any());
        verify(roomService).getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID);
    }

    @Test
    void updateTimer_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        UpdateTimerRequest request = new UpdateTimerRequest();
        request.setName("Updated Timer");

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/timers/{timerId}", TEST_ROOM_ID, TEST_TIMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService, never()).updateTimer(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void updateTimer_ShouldReturn404WhenTimerNotFound() throws Exception {
        // Given
        UpdateTimerRequest request = new UpdateTimerRequest();
        request.setName("Updated Timer");

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.updateTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), eq("Updated Timer"), any(), any())).thenReturn(false);

        // When & Then
        mockMvc.perform(put("/api/v1/rooms/{roomId}/timers/{timerId}", TEST_ROOM_ID, TEST_TIMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TIMER_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).updateTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), eq("Updated Timer"), any(), any());
    }

    @Test
    void deleteTimer_ShouldDeleteTimerSuccessfully() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.deleteTimer(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/v1/rooms/{roomId}/timers/{timerId}", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Timer deleted successfully"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).deleteTimer(TEST_ROOM_ID, TEST_TIMER_ID);
    }

    @Test
    void deleteTimer_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/v1/rooms/{roomId}/timers/{timerId}", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService, never()).deleteTimer(anyString(), anyString());
    }

    @Test
    void deleteTimer_ShouldReturn404WhenTimerNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.deleteTimer(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/v1/rooms/{roomId}/timers/{timerId}", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TIMER_NOT_FOUND"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).deleteTimer(TEST_ROOM_ID, TEST_TIMER_ID);
    }

    @Test
    void startTimer_ShouldStartTimerSuccessfully() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.startTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), any())).thenReturn(true);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/start", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_TIMER_ID));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).startTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), any());
        verify(roomService).getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID);
    }

    @Test
    void startTimer_ShouldStartWithCustomTime() throws Exception {
        // Given
        TimerControlRequest request = new TimerControlRequest();
        request.setStartTime(LocalDateTime.now().minusMinutes(5));

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.startTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), any(LocalDateTime.class))).thenReturn(true);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/start", TEST_ROOM_ID, TEST_TIMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(timerService).startTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), any(LocalDateTime.class));
    }

    @Test
    void pauseTimer_ShouldPauseTimerSuccessfully() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.pauseTimer(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(true);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/pause", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_TIMER_ID));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).pauseTimer(TEST_ROOM_ID, TEST_TIMER_ID);
        verify(roomService).getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID);
    }

    @Test
    void stopTimer_ShouldStopTimerSuccessfully() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.stopTimer(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(true);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/stop", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_TIMER_ID));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).stopTimer(TEST_ROOM_ID, TEST_TIMER_ID);
        verify(roomService).getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID);
    }

    @Test
    void resetTimer_ShouldResetTimerSuccessfully() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.resetTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), any())).thenReturn(true);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/reset", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_TIMER_ID));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).resetTimer(eq(TEST_ROOM_ID), eq(TEST_TIMER_ID), any());
        verify(roomService).getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID);
    }

    @Test
    void resetTimer_ShouldResetWithNewDuration() throws Exception {
        // Given
        TimerControlRequest request = new TimerControlRequest();
        request.setNewDuration(300000L);

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.resetTimer(TEST_ROOM_ID, TEST_TIMER_ID, 300000L)).thenReturn(true);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/reset", TEST_ROOM_ID, TEST_TIMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(timerService).resetTimer(TEST_ROOM_ID, TEST_TIMER_ID, 300000L);
    }

    @Test
    void adjustTimer_ShouldAdjustTimerSuccessfully() throws Exception {
        // Given
        TimerControlRequest request = new TimerControlRequest();
        request.setAdjustment(10000L);

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.adjustTimer(TEST_ROOM_ID, TEST_TIMER_ID, 10000L)).thenReturn(true);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/adjust", TEST_ROOM_ID, TEST_TIMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TEST_TIMER_ID));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService).adjustTimer(TEST_ROOM_ID, TEST_TIMER_ID, 10000L);
        verify(roomService).getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID);
    }

    @Test
    void adjustTimer_ShouldRequireAdjustmentValue() throws Exception {
        // Given
        TimerControlRequest request = new TimerControlRequest();
        // No adjustment value provided

        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/adjust", TEST_ROOM_ID, TEST_TIMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Adjustment value is required"));

        verify(roomService).roomExists(TEST_ROOM_ID);
        verify(timerService, never()).adjustTimer(anyString(), anyString(), anyLong());
    }

    @Test
    void timerControlOperations_ShouldReturn404WhenRoomNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(false);

        // When & Then - Test all control operations
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/start", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/pause", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/stop", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/reset", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        verify(roomService, times(4)).roomExists(TEST_ROOM_ID);
    }

    @Test
    void timerControlOperations_ShouldReturn404WhenTimerNotFound() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.startTimer(any(), any(), any())).thenReturn(false);
        when(timerService.pauseTimer(any(), any())).thenReturn(false);
        when(timerService.stopTimer(any(), any())).thenReturn(false);
        when(timerService.resetTimer(any(), any(), any())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/start", TEST_ROOM_ID, TEST_TIMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TIMER_NOT_FOUND"));
    }

    @Test
    void allEndpoints_ShouldHandleCorsHeaders() throws Exception {
        // Given
        when(roomService.roomExists(TEST_ROOM_ID)).thenReturn(true);
        when(timerService.startTimer(any(), any(), any())).thenReturn(true);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When & Then
        mockMvc.perform(post("/api/v1/rooms/{roomId}/timers/{timerId}/start", TEST_ROOM_ID, TEST_TIMER_ID)
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}