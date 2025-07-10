package com.timerfy.testutil;

import com.timerfy.model.*;

import java.time.LocalDateTime;

public class TestDataFactory {

    public static Room createTestRoom(String roomId) {
        Room room = new Room(roomId);
        room.setCreated(LocalDateTime.now());
        room.setLastActivity(LocalDateTime.now());
        room.setExpiresAt(LocalDateTime.now().plusDays(1));
        
        // Set default settings
        Room.RoomSettings settings = room.getSettings();
        settings.setMaxTimers(10);
        settings.setAllowViewerMessages(true);
        settings.setPrimaryColor("#007BFF");
        settings.setBackgroundColor("#FFFFFF");
        settings.setFontFamily("Arial");
        
        return room;
    }

    public static Timer createTestTimer(String name, long duration, TimerType type) {
        Timer timer = new Timer(name, duration, type);
        timer.setCreatedAt(LocalDateTime.now());
        
        // Set default settings
        Timer.TimerSettings settings = timer.getSettings();
        settings.setPlaySound(true);
        settings.setAutoReset(false);
        settings.setWarningTime(10000L); // 10 seconds
        settings.setCriticalTime(5000L);  // 5 seconds
        
        return timer;
    }

    public static Message createTestMessage(String text, MessagePriority priority, String color) {
        Message message = new Message(text, priority, color);
        message.setTimestamp(LocalDateTime.now());
        message.setCreatedBy("test-user");
        return message;
    }

    public static Timer createCountdownTimer(String name, long duration) {
        Timer timer = createTestTimer(name, duration, TimerType.COUNTDOWN);
        timer.setCurrentTime(duration); // Start at full duration for countdown
        return timer;
    }

    public static Timer createStopwatchTimer(String name) {
        Timer timer = createTestTimer(name, 0L, TimerType.STOPWATCH);
        timer.setCurrentTime(0L); // Start at zero for stopwatch
        return timer;
    }

    public static Room createRoomWithTimers(String roomId, int timerCount) {
        Room room = createTestRoom(roomId);
        
        for (int i = 1; i <= timerCount; i++) {
            Timer timer = createCountdownTimer("Timer " + i, 60000L * i);
            room.addTimer(timer);
        }
        
        return room;
    }

    public static Room createRoomWithMessages(String roomId, int messageCount) {
        Room room = createTestRoom(roomId);
        
        MessagePriority[] priorities = {MessagePriority.LOW, MessagePriority.NORMAL, MessagePriority.HIGH, MessagePriority.CRITICAL};
        String[] colors = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00"};
        
        for (int i = 1; i <= messageCount; i++) {
            Message message = createTestMessage(
                "Test message " + i,
                priorities[i % priorities.length],
                colors[i % colors.length]
            );
            room.addMessage(message);
        }
        
        return room;
    }

    public static Timer createRunningTimer(String name, long duration) {
        Timer timer = createCountdownTimer(name, duration);
        timer.setState(TimerState.RUNNING);
        timer.setStartedAt(LocalDateTime.now());
        return timer;
    }

    public static Timer createPausedTimer(String name, long duration, long currentTime) {
        Timer timer = createCountdownTimer(name, duration);
        timer.setState(TimerState.PAUSED);
        timer.setStartedAt(LocalDateTime.now().minusMinutes(1));
        timer.setPausedAt(LocalDateTime.now());
        timer.setCurrentTime(currentTime);
        return timer;
    }

    public static Timer createCompletedTimer(String name, long duration) {
        Timer timer = createCountdownTimer(name, duration);
        timer.setState(TimerState.COMPLETED);
        timer.setStartedAt(LocalDateTime.now().minusMinutes(1));
        timer.setCompletedAt(LocalDateTime.now());
        timer.setCurrentTime(0L);
        return timer;
    }

    public static Message createHiddenMessage(String text) {
        Message message = createTestMessage(text, MessagePriority.NORMAL, "#000000");
        message.setVisible(false);
        return message;
    }

    public static Message createAutoHideMessage(String text, long duration) {
        Message message = createTestMessage(text, MessagePriority.NORMAL, "#000000");
        message.setAutoShow(true);
        message.setDisplayDuration(duration);
        message.setAutoHideAt(LocalDateTime.now().plusSeconds(duration / 1000));
        return message;
    }

    public static Message createExpiredMessage(String text) {
        Message message = createAutoHideMessage(text, 1000L);
        message.setAutoHideAt(LocalDateTime.now().minusMinutes(1)); // Already expired
        return message;
    }

    public static Room createFullRoom(String roomId) {
        Room room = createTestRoom(roomId);
        room.getSettings().setMaxTimers(2);
        
        // Add maximum timers
        room.addTimer(createRunningTimer("Timer 1", 60000L));
        room.addTimer(createPausedTimer("Timer 2", 120000L, 30000L));
        
        // Add some messages
        room.addMessage(createTestMessage("Welcome message", MessagePriority.HIGH, "#00FF00"));
        room.addMessage(createHiddenMessage("Hidden info"));
        room.addMessage(createAutoHideMessage("Temporary notice", 5000L));
        
        // Update stats
        room.getStats().setConnectedUsers(5);
        room.getStats().setTotalControllers(2);
        room.getStats().setTotalViewers(3);
        
        return room;
    }

    public static Room createExpiredRoom(String roomId) {
        Room room = createTestRoom(roomId);
        room.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        return room;
    }
}