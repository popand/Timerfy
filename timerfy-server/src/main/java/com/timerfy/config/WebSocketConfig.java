package com.timerfy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.lang.NonNull;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Value("${websocket.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;
    
    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
    
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Enable simple message broker with destination prefixes
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{10000, 10000}) // 10 seconds heartbeat
              .setTaskScheduler(heartBeatScheduler()); // Add TaskScheduler for heartbeat
        
        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for private messages
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Register STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js")
                .setHeartbeatTime(25000); // 25 seconds SockJS heartbeat
        
        // Register STOMP endpoint without SockJS for native WebSocket clients
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
    }
}