package com.timerfy.service;

import com.timerfy.model.Timer;
import com.timerfy.model.TimerState;
import com.timerfy.model.TimerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TimerService {
    
    private static final Logger logger = LoggerFactory.getLogger(TimerService.class);
    
    private final Map<String, ScheduledFuture<?>> runningTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    
    @Autowired
    private RoomService roomService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public Timer createTimer(String roomId, String name, long duration, TimerType type) {
        if (!roomService.canAddTimer(roomId)) {
            throw new IllegalStateException("Cannot add more timers to room " + roomId);
        }
        
        Timer timer = new Timer(name, duration, type);
        
        if (roomService.addTimerToRoom(roomId, timer)) {
            logger.info("Created timer {} in room {}", timer.getId(), roomId);
            publishTimerEvent(roomId, timer, "TIMER_CREATED");
            return timer;
        }
        
        throw new RuntimeException("Failed to create timer in room " + roomId);
    }
    
    public boolean updateTimer(String roomId, String timerId, String name, Long duration, Timer.TimerSettings settings) {
        Optional<Timer> timerOpt = roomService.getTimerFromRoom(roomId, timerId);
        
        if (timerOpt.isEmpty()) {
            return false;
        }
        
        Timer timer = timerOpt.get();
        
        synchronized (timer) {
            boolean wasRunning = timer.getState() == TimerState.RUNNING;
            
            if (wasRunning) {
                pauseTimer(roomId, timerId);
            }
            
            if (name != null) {
                timer.setName(name);
            }
            
            if (duration != null) {
                timer.setDuration(duration);
                if (timer.getState() == TimerState.STOPPED) {
                    timer.setCurrentTime(timer.getType() == TimerType.COUNTDOWN ? duration : 0);
                }
            }
            
            if (settings != null) {
                timer.setSettings(settings);
            }
            
            roomService.updateTimerInRoom(roomId, timer);
            
            if (wasRunning) {
                startTimer(roomId, timerId, null);
            }
            
            logger.info("Updated timer {} in room {}", timerId, roomId);
            publishTimerEvent(roomId, timer, "TIMER_UPDATED");
            return true;
        }
    }
    
    public boolean startTimer(String roomId, String timerId, LocalDateTime startTime) {
        Optional<Timer> timerOpt = roomService.getTimerFromRoom(roomId, timerId);
        
        if (timerOpt.isEmpty()) {
            return false;
        }
        
        Timer timer = timerOpt.get();
        
        synchronized (timer) {
            if (timer.getState() == TimerState.RUNNING) {
                return false;
            }
            
            timer.start();
            
            if (startTime != null) {
                timer.setStartedAt(startTime);
            }
            
            roomService.updateTimerInRoom(roomId, timer);
            startTimerTicking(roomId, timer);
            
            logger.info("Started timer {} in room {}", timerId, roomId);
            publishTimerEvent(roomId, timer, "TIMER_STARTED");
            return true;
        }
    }
    
    public boolean pauseTimer(String roomId, String timerId) {
        Optional<Timer> timerOpt = roomService.getTimerFromRoom(roomId, timerId);
        
        if (timerOpt.isEmpty()) {
            return false;
        }
        
        Timer timer = timerOpt.get();
        
        synchronized (timer) {
            if (timer.getState() != TimerState.RUNNING) {
                return false;
            }
            
            timer.pause();
            roomService.updateTimerInRoom(roomId, timer);
            stopTimerTicking(timer.getId());
            
            logger.info("Paused timer {} in room {}", timerId, roomId);
            publishTimerEvent(roomId, timer, "TIMER_PAUSED");
            return true;
        }
    }
    
    public boolean stopTimer(String roomId, String timerId) {
        Optional<Timer> timerOpt = roomService.getTimerFromRoom(roomId, timerId);
        
        if (timerOpt.isEmpty()) {
            return false;
        }
        
        Timer timer = timerOpt.get();
        
        synchronized (timer) {
            timer.stop();
            roomService.updateTimerInRoom(roomId, timer);
            stopTimerTicking(timer.getId());
            
            logger.info("Stopped timer {} in room {}", timerId, roomId);
            publishTimerEvent(roomId, timer, "TIMER_STOPPED");
            return true;
        }
    }
    
    public boolean resetTimer(String roomId, String timerId, Long newDuration) {
        Optional<Timer> timerOpt = roomService.getTimerFromRoom(roomId, timerId);
        
        if (timerOpt.isEmpty()) {
            return false;
        }
        
        Timer timer = timerOpt.get();
        
        synchronized (timer) {
            stopTimerTicking(timer.getId());
            
            if (newDuration != null) {
                timer.reset(newDuration);
            } else {
                timer.reset();
            }
            
            roomService.updateTimerInRoom(roomId, timer);
            
            logger.info("Reset timer {} in room {}", timerId, roomId);
            publishTimerEvent(roomId, timer, "TIMER_RESET");
            return true;
        }
    }
    
    public boolean adjustTimer(String roomId, String timerId, long adjustment) {
        Optional<Timer> timerOpt = roomService.getTimerFromRoom(roomId, timerId);
        
        if (timerOpt.isEmpty()) {
            return false;
        }
        
        Timer timer = timerOpt.get();
        
        synchronized (timer) {
            timer.adjustTime(adjustment);
            roomService.updateTimerInRoom(roomId, timer);
            
            logger.info("Adjusted timer {} in room {} by {} seconds", timerId, roomId, adjustment);
            publishTimerEvent(roomId, timer, "TIMER_ADJUSTED");
            return true;
        }
    }
    
    public boolean deleteTimer(String roomId, String timerId) {
        Optional<Timer> timerOpt = roomService.getTimerFromRoom(roomId, timerId);
        
        if (timerOpt.isEmpty()) {
            return false;
        }
        
        Timer timer = timerOpt.get();
        stopTimerTicking(timer.getId());
        
        if (roomService.removeTimerFromRoom(roomId, timerId)) {
            logger.info("Deleted timer {} from room {}", timerId, roomId);
            publishTimerEvent(roomId, timer, "TIMER_DELETED");
            return true;
        }
        
        return false;
    }
    
    @Async
    public void startTimerTicking(String roomId, Timer timer) {
        String timerId = timer.getId();
        
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                Optional<Timer> currentTimerOpt = roomService.getTimerFromRoom(roomId, timerId);
                
                if (currentTimerOpt.isEmpty()) {
                    stopTimerTicking(timerId);
                    return;
                }
                
                Timer currentTimer = currentTimerOpt.get();
                
                synchronized (currentTimer) {
                    if (currentTimer.getState() != TimerState.RUNNING) {
                        stopTimerTicking(timerId);
                        return;
                    }
                    
                    boolean wasInWarning = currentTimer.isInWarningState();
                    boolean wasInCritical = currentTimer.isInCriticalState();
                    
                    currentTimer.tick();
                    
                    checkWarningStates(roomId, currentTimer, wasInWarning, wasInCritical);
                    
                    if (currentTimer.isCompleted()) {
                        handleTimerCompletion(roomId, currentTimer);
                    }
                    
                    roomService.updateTimerInRoom(roomId, currentTimer);
                    publishTimerEvent(roomId, currentTimer, "TIMER_TICK");
                }
            } catch (Exception e) {
                logger.error("Error in timer tick for timer {} in room {}: {}", timerId, roomId, e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        runningTimers.put(timerId, future);
        logger.debug("Started ticking for timer {} in room {}", timerId, roomId);
    }
    
    private void stopTimerTicking(String timerId) {
        ScheduledFuture<?> future = runningTimers.remove(timerId);
        if (future != null) {
            future.cancel(false);
            logger.debug("Stopped ticking for timer {}", timerId);
        }
    }
    
    private void checkWarningStates(String roomId, Timer timer, boolean wasInWarning, boolean wasInCritical) {
        boolean isInWarning = timer.isInWarningState();
        boolean isInCritical = timer.isInCriticalState();
        
        if (isInCritical && !wasInCritical) {
            logger.info("Timer {} in room {} entered critical state", timer.getId(), roomId);
            publishTimerEvent(roomId, timer, "TIMER_CRITICAL");
        } else if (isInWarning && !wasInWarning && !isInCritical) {
            logger.info("Timer {} in room {} entered warning state", timer.getId(), roomId);
            publishTimerEvent(roomId, timer, "TIMER_WARNING");
        }
    }
    
    private void handleTimerCompletion(String roomId, Timer timer) {
        timer.setState(TimerState.COMPLETED);
        timer.setCompletedAt(LocalDateTime.now());
        
        stopTimerTicking(timer.getId());
        
        logger.info("Timer {} in room {} completed", timer.getId(), roomId);
        publishTimerEvent(roomId, timer, "TIMER_COMPLETED");
        
        if (timer.getSettings().getAutoReset()) {
            logger.info("Auto-resetting timer {} in room {}", timer.getId(), roomId);
            resetTimer(roomId, timer.getId(), null);
        }
    }
    
    private void publishTimerEvent(String roomId, Timer timer, String eventType) {
        try {
            TimerEvent event = new TimerEvent(roomId, timer, eventType);
            eventPublisher.publishEvent(event);
            logger.debug("Published timer event: {} for timer {} in room {}", eventType, timer.getId(), roomId);
        } catch (Exception e) {
            logger.error("Failed to publish timer event: {}", e.getMessage());
        }
    }
    
    public void stopAllTimersInRoom(String roomId) {
        Optional<com.timerfy.model.Room> roomOpt = roomService.getRoomById(roomId);
        
        if (roomOpt.isEmpty()) {
            return;
        }
        
        com.timerfy.model.Room room = roomOpt.get();
        
        for (Timer timer : room.getTimers()) {
            if (timer.getState() == TimerState.RUNNING) {
                stopTimer(roomId, timer.getId());
            }
        }
        
        logger.info("Stopped all timers in room {}", roomId);
    }
    
    public static class TimerEvent {
        private final String roomId;
        private final Timer timer;
        private final String eventType;
        private final LocalDateTime timestamp;
        
        public TimerEvent(String roomId, Timer timer, String eventType) {
            this.roomId = roomId;
            this.timer = timer;
            this.eventType = eventType;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getRoomId() { return roomId; }
        public Timer getTimer() { return timer; }
        public String getEventType() { return eventType; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}