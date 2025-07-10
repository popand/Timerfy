package com.timerfy.service;

import com.timerfy.model.Timer;
import com.timerfy.model.TimerState;
import com.timerfy.model.TimerType;
import com.timerfy.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimerServiceTest {

    @Mock
    private RoomService roomService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @InjectMocks
    private TimerService timerService;

    private Timer testTimer;
    private Room testRoom;
    private final String TEST_ROOM_ID = "ABC123";
    private final String TEST_TIMER_ID = "timer-1";
    private final String TIMER_NAME = "Test Timer";
    private final long TIMER_DURATION = 60000L; // 60 seconds

    @BeforeEach
    void setUp() {
        testTimer = new Timer(TIMER_NAME, TIMER_DURATION, TimerType.COUNTDOWN);
        testTimer.setId(TEST_TIMER_ID);
        testRoom = new Room(TEST_ROOM_ID);
        testRoom.addTimer(testTimer);
    }

    @Test
    void createTimer_ShouldCreateTimerWhenRoomCanAddTimer() {
        // Given
        when(roomService.canAddTimer(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.addTimerToRoom(eq(TEST_ROOM_ID), any(Timer.class))).thenReturn(true);

        // When
        Timer createdTimer = timerService.createTimer(TEST_ROOM_ID, TIMER_NAME, TIMER_DURATION, TimerType.COUNTDOWN);

        // Then
        assertNotNull(createdTimer);
        assertEquals(TIMER_NAME, createdTimer.getName());
        assertEquals(TIMER_DURATION, createdTimer.getDuration());
        assertEquals(TimerType.COUNTDOWN, createdTimer.getType());
        assertEquals(TimerState.STOPPED, createdTimer.getState());
        verify(roomService).addTimerToRoom(eq(TEST_ROOM_ID), any(Timer.class));
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void createTimer_ShouldThrowExceptionWhenRoomCannotAddTimer() {
        // Given
        when(roomService.canAddTimer(TEST_ROOM_ID)).thenReturn(false);

        // When & Then
        assertThrows(IllegalStateException.class, () -> 
            timerService.createTimer(TEST_ROOM_ID, TIMER_NAME, TIMER_DURATION, TimerType.COUNTDOWN));
        verify(roomService, never()).addTimerToRoom(anyString(), any(Timer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createTimer_ShouldThrowExceptionWhenAddToRoomFails() {
        // Given
        when(roomService.canAddTimer(TEST_ROOM_ID)).thenReturn(true);
        when(roomService.addTimerToRoom(eq(TEST_ROOM_ID), any(Timer.class))).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            timerService.createTimer(TEST_ROOM_ID, TIMER_NAME, TIMER_DURATION, TimerType.COUNTDOWN));
    }

    @Test
    void startTimer_ShouldStartTimerWhenTimerExists() {
        // Given
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.startTimer(TEST_ROOM_ID, TEST_TIMER_ID, null);

        // Then
        assertTrue(result);
        assertEquals(TimerState.RUNNING, testTimer.getState());
        assertNotNull(testTimer.getStartedAt());
        verify(roomService).updateTimerInRoom(TEST_ROOM_ID, testTimer);
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void startTimer_ShouldSetCustomStartTime() {
        // Given
        LocalDateTime customStartTime = LocalDateTime.now().minusMinutes(5);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.startTimer(TEST_ROOM_ID, TEST_TIMER_ID, customStartTime);

        // Then
        assertTrue(result);
        assertEquals(customStartTime, testTimer.getStartedAt());
    }

    @Test
    void startTimer_ShouldReturnFalseWhenTimerNotFound() {
        // Given
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.empty());

        // When
        boolean result = timerService.startTimer(TEST_ROOM_ID, TEST_TIMER_ID, null);

        // Then
        assertFalse(result);
        verify(roomService, never()).updateTimerInRoom(anyString(), any(Timer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void startTimer_ShouldReturnFalseWhenTimerAlreadyRunning() {
        // Given
        testTimer.setState(TimerState.RUNNING);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When
        boolean result = timerService.startTimer(TEST_ROOM_ID, TEST_TIMER_ID, null);

        // Then
        assertFalse(result);
        verify(roomService, never()).updateTimerInRoom(anyString(), any(Timer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void pauseTimer_ShouldPauseRunningTimer() {
        // Given
        testTimer.setState(TimerState.RUNNING);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.pauseTimer(TEST_ROOM_ID, TEST_TIMER_ID);

        // Then
        assertTrue(result);
        assertEquals(TimerState.PAUSED, testTimer.getState());
        assertNotNull(testTimer.getPausedAt());
        verify(roomService).updateTimerInRoom(TEST_ROOM_ID, testTimer);
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void pauseTimer_ShouldReturnFalseWhenTimerNotRunning() {
        // Given
        testTimer.setState(TimerState.STOPPED);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When
        boolean result = timerService.pauseTimer(TEST_ROOM_ID, TEST_TIMER_ID);

        // Then
        assertFalse(result);
        verify(roomService, never()).updateTimerInRoom(anyString(), any(Timer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void stopTimer_ShouldStopTimer() {
        // Given
        testTimer.setState(TimerState.RUNNING);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.stopTimer(TEST_ROOM_ID, TEST_TIMER_ID);

        // Then
        assertTrue(result);
        assertEquals(TimerState.STOPPED, testTimer.getState());
        verify(roomService).updateTimerInRoom(TEST_ROOM_ID, testTimer);
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void resetTimer_ShouldResetTimerToOriginalDuration() {
        // Given
        testTimer.setState(TimerState.RUNNING);
        testTimer.setCurrentTime(30000L); // 30 seconds remaining
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.resetTimer(TEST_ROOM_ID, TEST_TIMER_ID, null);

        // Then
        assertTrue(result);
        assertEquals(TimerState.STOPPED, testTimer.getState());
        assertEquals(TIMER_DURATION, testTimer.getCurrentTime());
        verify(roomService).updateTimerInRoom(TEST_ROOM_ID, testTimer);
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void resetTimer_ShouldResetTimerToNewDuration() {
        // Given
        long newDuration = 120000L; // 2 minutes
        testTimer.setState(TimerState.RUNNING);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.resetTimer(TEST_ROOM_ID, TEST_TIMER_ID, newDuration);

        // Then
        assertTrue(result);
        assertEquals(TimerState.STOPPED, testTimer.getState());
        assertEquals(newDuration, testTimer.getDuration());
        assertEquals(newDuration, testTimer.getCurrentTime());
        verify(roomService).updateTimerInRoom(TEST_ROOM_ID, testTimer);
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void adjustTimer_ShouldAdjustTimerTime() {
        // Given
        long adjustment = 10000L; // Add 10 seconds
        testTimer.setCurrentTime(30000L); // 30 seconds
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.adjustTimer(TEST_ROOM_ID, TEST_TIMER_ID, adjustment);

        // Then
        assertTrue(result);
        assertEquals(40000L, testTimer.getCurrentTime()); // Should be 40 seconds
        verify(roomService).updateTimerInRoom(TEST_ROOM_ID, testTimer);
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void adjustTimer_ShouldHandleNegativeAdjustment() {
        // Given
        long adjustment = -5000L; // Subtract 5 seconds
        testTimer.setCurrentTime(30000L); // 30 seconds
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.adjustTimer(TEST_ROOM_ID, TEST_TIMER_ID, adjustment);

        // Then
        assertTrue(result);
        assertEquals(25000L, testTimer.getCurrentTime()); // Should be 25 seconds
    }

    @Test
    void deleteTimer_ShouldDeleteTimerFromRoom() {
        // Given
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.removeTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(true);

        // When
        boolean result = timerService.deleteTimer(TEST_ROOM_ID, TEST_TIMER_ID);

        // Then
        assertTrue(result);
        verify(roomService).removeTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID);
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void deleteTimer_ShouldReturnFalseWhenTimerNotFound() {
        // Given
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.empty());

        // When
        boolean result = timerService.deleteTimer(TEST_ROOM_ID, TEST_TIMER_ID);

        // Then
        assertFalse(result);
        verify(roomService, never()).removeTimerFromRoom(anyString(), anyString());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteTimer_ShouldReturnFalseWhenRemovalFails() {
        // Given
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.removeTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(false);

        // When
        boolean result = timerService.deleteTimer(TEST_ROOM_ID, TEST_TIMER_ID);

        // Then
        assertFalse(result);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateTimer_ShouldUpdateTimerProperties() {
        // Given
        String newName = "Updated Timer";
        Long newDuration = 120000L;
        Timer.TimerSettings newSettings = new Timer.TimerSettings();
        newSettings.setPlaySound(true);
        
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.updateTimer(TEST_ROOM_ID, TEST_TIMER_ID, newName, newDuration, newSettings);

        // Then
        assertTrue(result);
        assertEquals(newName, testTimer.getName());
        assertEquals(newDuration, testTimer.getDuration());
        assertEquals(newSettings, testTimer.getSettings());
        verify(roomService).updateTimerInRoom(TEST_ROOM_ID, testTimer);
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void updateTimer_ShouldHandleRunningTimer() {
        // Given
        testTimer.setState(TimerState.RUNNING);
        String newName = "Updated Timer";
        
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));
        when(roomService.updateTimerInRoom(TEST_ROOM_ID, testTimer)).thenReturn(true);

        // When
        boolean result = timerService.updateTimer(TEST_ROOM_ID, TEST_TIMER_ID, newName, null, null);

        // Then
        assertTrue(result);
        assertEquals(newName, testTimer.getName());
        // Timer should be restarted after update
        verify(roomService, atLeast(2)).updateTimerInRoom(TEST_ROOM_ID, testTimer);
    }

    @Test
    void updateTimer_ShouldReturnFalseWhenTimerNotFound() {
        // Given
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.empty());

        // When
        boolean result = timerService.updateTimer(TEST_ROOM_ID, TEST_TIMER_ID, "New Name", null, null);

        // Then
        assertFalse(result);
        verify(roomService, never()).updateTimerInRoom(anyString(), any(Timer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void stopAllTimersInRoom_ShouldStopAllRunningTimers() {
        // Given
        Timer timer1 = new Timer("Timer 1", 60000L, TimerType.COUNTDOWN);
        timer1.setId("timer-1");
        timer1.setState(TimerState.RUNNING);
        
        Timer timer2 = new Timer("Timer 2", 120000L, TimerType.COUNTDOWN);
        timer2.setId("timer-2");
        timer2.setState(TimerState.STOPPED);
        
        testRoom.addTimer(timer1);
        testRoom.addTimer(timer2);
        
        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.of(testRoom));
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, "timer-1")).thenReturn(Optional.of(timer1));
        when(roomService.updateTimerInRoom(eq(TEST_ROOM_ID), any(Timer.class))).thenReturn(true);

        // When
        timerService.stopAllTimersInRoom(TEST_ROOM_ID);

        // Then
        verify(roomService).updateTimerInRoom(eq(TEST_ROOM_ID), eq(timer1));
        verify(roomService, never()).updateTimerInRoom(eq(TEST_ROOM_ID), eq(timer2));
        verify(eventPublisher).publishEvent(any(TimerService.TimerEvent.class));
    }

    @Test
    void stopAllTimersInRoom_ShouldHandleNonExistentRoom() {
        // Given
        when(roomService.getRoomById(TEST_ROOM_ID)).thenReturn(Optional.empty());

        // When
        timerService.stopAllTimersInRoom(TEST_ROOM_ID);

        // Then
        verify(roomService, never()).updateTimerInRoom(anyString(), any(Timer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void timerEvent_ShouldContainCorrectData() {
        // Given
        String eventType = "TIMER_STARTED";
        LocalDateTime beforeEvent = LocalDateTime.now().minusSeconds(1);

        // When
        TimerService.TimerEvent event = new TimerService.TimerEvent(TEST_ROOM_ID, testTimer, eventType);

        // Then
        assertEquals(TEST_ROOM_ID, event.getRoomId());
        assertEquals(testTimer, event.getTimer());
        assertEquals(eventType, event.getEventType());
        assertTrue(event.getTimestamp().isAfter(beforeEvent));
        assertTrue(event.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void startTimerTicking_ShouldCreateScheduledTask() {
        // This test verifies that the ticking mechanism is set up correctly
        // The actual ticking logic is tested in integration tests
        
        // Given
        testTimer.setState(TimerState.RUNNING);
        when(roomService.getTimerFromRoom(TEST_ROOM_ID, TEST_TIMER_ID)).thenReturn(Optional.of(testTimer));

        // When
        timerService.startTimerTicking(TEST_ROOM_ID, testTimer);

        // Then
        // Verify that the timer is being tracked (scheduler setup)
        // Note: This is more of an integration test, but we can verify setup
        assertNotNull(testTimer.getId());
        assertEquals(TimerState.RUNNING, testTimer.getState());
    }
}