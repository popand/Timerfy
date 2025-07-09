# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **Timerfy**, a distributed countdown timer system with a decoupled client-server architecture. The focus is currently on the **server implementation only** - a Spring Boot application that provides RESTful APIs and WebSocket connections for real-time timer management.

## Development Commands

### Maven Commands
```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run

# Run tests
mvn test

# Package the application
mvn package

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Redis Setup
```bash
# Start Redis server (required for development)
redis-server

# Check Redis connection
redis-cli ping
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=RoomServiceTest

# Run integration tests
mvn test -Dtest=*IntegrationTest
```

## Architecture Overview

The server follows Spring Boot's layered architecture with these key packages:

- **`com.timerfy.controller`**: REST API endpoints following RESTful principles
- **`com.timerfy.service`**: Business logic services (RoomService, TimerService, MessageService)
- **`com.timerfy.model`**: Entity models for Room, Timer, Message
- **`com.timerfy.dto`**: Data Transfer Objects for API requests/responses
- **`com.timerfy.config`**: Spring configuration classes (WebSocket, Redis, CORS)
- **`com.timerfy.websocket`**: WebSocket handlers for real-time communication
- **`com.timerfy.exception`**: Global exception handling with @ControllerAdvice

## Key Technical Decisions

### Data Storage
- **Redis** is used for all state management (rooms, timers, messages)
- No traditional database - stateless server design for horizontal scaling
- Room expiration handled via Redis TTL (24 hours default)

### Real-time Communication
- **WebSocket with STOMP protocol** for real-time updates
- Spring's `SimpMessagingTemplate` for broadcasting events
- Room-specific subscriptions via `/topic/room/{roomId}`

### API Design
- RESTful endpoints under `/api/v1/`
- Consistent response format with `success`, `data`, `error` fields
- OpenAPI documentation available at `/swagger-ui.html`

## Development Patterns

### Service Layer
- Use `@Service` annotation for business logic
- Inject `RedisTemplate` for data operations
- Use `ApplicationEventPublisher` for WebSocket events
- Implement validation with Bean Validation annotations

### Controller Layer
- Use `@RestController` for REST endpoints
- Validate input with `@Valid` annotations
- Return consistent `ResponseEntity` objects
- Handle exceptions with `@ControllerAdvice`

### WebSocket Implementation
- Configure with `@EnableWebSocketMessageBroker`
- Use `@MessageMapping` for client messages
- Use `@SendTo` for broadcasting to topics
- Handle connections with `@EventListener`

## Important Configuration

### CORS Settings
Configured in `application.yml` for client origins:
```yaml
spring:
  web:
    cors:
      allowed-origins: "http://localhost:3000,http://localhost:5173"
```

### Redis Configuration
Connection pooling and timeout settings in `application.yml`:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

### Room Management
- Room IDs are 6-character alphanumeric strings
- Automatic cleanup after 24 hours
- Maximum 10 timers per room by default
- Maximum 50 users per room

## API Endpoints Structure

### Room Management
- `POST /api/v1/rooms` - Create new room
- `GET /api/v1/rooms/{roomId}` - Get room details
- `DELETE /api/v1/rooms/{roomId}` - Delete room
- `GET /api/v1/rooms/{roomId}/status` - Get room status

### Timer Management
- `POST /api/v1/rooms/{roomId}/timers` - Create timer
- `PUT /api/v1/rooms/{roomId}/timers/{timerId}` - Update timer
- `DELETE /api/v1/rooms/{roomId}/timers/{timerId}` - Delete timer

### Timer Control
- `POST /api/v1/rooms/{roomId}/timers/{timerId}/start` - Start timer
- `POST /api/v1/rooms/{roomId}/timers/{timerId}/stop` - Stop timer
- `POST /api/v1/rooms/{roomId}/timers/{timerId}/pause` - Pause timer
- `POST /api/v1/rooms/{roomId}/timers/{timerId}/reset` - Reset timer

### Message Management
- `POST /api/v1/rooms/{roomId}/messages` - Create message
- `PUT /api/v1/rooms/{roomId}/messages/{messageId}` - Update message
- `DELETE /api/v1/rooms/{roomId}/messages/{messageId}` - Delete message

## WebSocket Events

### Connection
- Client connects to `/app/room/{roomId}`
- Subscribe to `/topic/room/{roomId}` for updates

### Event Types
- `ROOM_JOINED` - User joined room
- `TIMER_UPDATED` - Timer state changed
- `TIMER_STARTED/STOPPED/PAUSED` - Timer control events
- `MESSAGE_CREATED/UPDATED/DELETED` - Message events
- `ERROR` - Error notifications

## Current Development Status

The project is in initial setup phase. The basic Spring Boot structure is created with:
- Maven configuration with required dependencies
- Application configuration in `application.yml`
- Main application class with proper annotations
- Package structure for organized development

## Implementation Milestones

### MILESTONE 1: Server Foundation (Week 1) - ✅ COMPLETED
- [x] Spring Boot project setup with Java 17+ and required dependencies
- [x] Maven configuration with Web, WebSocket, Data Redis, Validation, Actuator
- [x] Redis connection configuration using Spring Data Redis
- [x] CORS settings for cross-origin requests
- [x] Application.yml with environment-specific configurations
- [x] Basic project structure with packages (controller, service, model, dto, config)
- [x] Spring Boot Actuator configuration for health checks

### MILESTONE 2: Data Models and Core Services (Week 2)
#### Task 2.1: Data Models and DTOs
- [ ] Create Room entity/model class with validation annotations
- [ ] Create Timer entity/model class with state management
- [ ] Create Message entity/model class with priority handling
- [ ] Define enum classes: TimerState, TimerType, MessagePriority, UserRole
- [ ] Create DTOs for API requests and responses with Bean Validation
- [ ] Configure JSON serialization settings for date/time handling

#### Task 2.2: Room Management Service
- [ ] Create RoomService class with @Service annotation
- [ ] Implement room CRUD operations using RedisTemplate
- [ ] Create room ID generation utility (6-character alphanumeric)
- [ ] Add room expiration logic using Redis TTL
- [ ] Implement room validation using Bean Validation
- [ ] Add room statistics tracking and updates
- [ ] Create scheduled cleanup service using @Scheduled

#### Task 2.3: Timer Management Service
- [ ] Create TimerService class for timer operations
- [ ] Implement timer state management with thread-safe operations
- [ ] Add timer tick mechanism using Spring's @Async and ScheduledExecutorService
- [ ] Create timer validation logic with custom validators
- [ ] Add timer event publishing using Spring's ApplicationEventPublisher
- [ ] Implement timer persistence using Redis operations
- [ ] Create timer warning/critical time detection

### MILESTONE 3: REST API Implementation (Week 3)
#### Task 3.1: REST API Controllers
- [ ] Create RoomController with @RestController annotation
- [ ] Implement TimerController for timer management endpoints
- [ ] Add MessageController for message operations
- [ ] Create request/response DTOs with validation annotations
- [ ] Add global exception handling with @ControllerAdvice
- [ ] Implement rate limiting using Spring AOP or interceptors
- [ ] Configure OpenAPI documentation with SpringDoc

#### Task 3.2: API Endpoints Implementation
- [ ] POST /api/v1/rooms - Create new room
- [ ] GET /api/v1/rooms/{roomId} - Get room details and current state
- [ ] DELETE /api/v1/rooms/{roomId} - Delete room (controller only)
- [ ] GET /api/v1/rooms/{roomId}/status - Get lightweight room status
- [ ] POST /api/v1/rooms/{roomId}/timers - Create timer
- [ ] PUT /api/v1/rooms/{roomId}/timers/{timerId} - Update timer
- [ ] DELETE /api/v1/rooms/{roomId}/timers/{timerId} - Delete timer
- [ ] POST /api/v1/rooms/{roomId}/timers/{timerId}/start - Start timer
- [ ] POST /api/v1/rooms/{roomId}/timers/{timerId}/stop - Stop timer
- [ ] POST /api/v1/rooms/{roomId}/timers/{timerId}/pause - Pause timer
- [ ] POST /api/v1/rooms/{roomId}/timers/{timerId}/reset - Reset timer
- [ ] POST /api/v1/rooms/{roomId}/timers/{timerId}/adjust - Adjust timer time

### MILESTONE 4: WebSocket Implementation (Week 4)
#### Task 4.1: WebSocket Configuration and Handler
- [ ] Create WebSocket configuration class with @EnableWebSocketMessageBroker
- [ ] Configure STOMP endpoints and message broker
- [ ] Create WebSocketHandler for connection management
- [ ] Implement room joining/leaving logic with STOMP subscriptions
- [ ] Add connection state management using WebSocket sessions
- [ ] Create heartbeat mechanism using STOMP heartbeat frames
- [ ] Implement user role management and authorization

#### Task 4.2: Timer Event Broadcasting
- [ ] Create TimerEventListener using @EventListener
- [ ] Implement real-time timer updates using SimpMessagingTemplate
- [ ] Add timer state change notifications to WebSocket subscribers
- [ ] Create timer warning/critical alerts broadcasting
- [ ] Add timer completion events with sound notification support
- [ ] Implement event rate limiting and message queuing
- [ ] Create timer event DTOs for WebSocket payloads

#### Task 4.3: Message Event System
- [ ] Create MessageEventListener for message broadcasting
- [ ] Implement message visibility events using WebSocket
- [ ] Add message creation/update/delete event broadcasting
- [ ] Create message priority handling and routing
- [ ] Add auto-hide message functionality with scheduled tasks
- [ ] Implement message queue management for high-priority messages
- [ ] Create message event DTOs and validation

### MILESTONE 5: Message System (Week 5)
#### Task 5.1: Message Management Service
- [ ] Create MessageService class with @Service annotation
- [ ] Add message API endpoints in MessageController
- [ ] Implement message WebSocket events using ApplicationEventPublisher
- [ ] Add message validation and sanitization using Bean Validation
- [ ] Create message auto-hide functionality using @Scheduled tasks
- [ ] Add message priority handling and routing logic
- [ ] Implement message persistence using Redis operations

#### Task 5.2: Message API Endpoints
- [ ] POST /api/v1/rooms/{roomId}/messages - Create message
- [ ] PUT /api/v1/rooms/{roomId}/messages/{messageId} - Update message
- [ ] DELETE /api/v1/rooms/{roomId}/messages/{messageId} - Delete message
- [ ] POST /api/v1/rooms/{roomId}/messages/{messageId}/show - Show message
- [ ] POST /api/v1/rooms/{roomId}/messages/{messageId}/hide - Hide message
- [ ] PUT /api/v1/rooms/{roomId}/settings - Update room settings

### MILESTONE 6: Testing and Quality Assurance (Week 6)
#### Task 6.1: Unit Testing
- [ ] Write JUnit tests for RoomService with Redis operations
- [ ] Create unit tests for TimerService with mock dependencies
- [ ] Add unit tests for MessageService functionality
- [ ] Test controller endpoints with MockMvc
- [ ] Create test utilities for Redis test containers
- [ ] Add validation testing for DTOs and entities

#### Task 6.2: Integration Testing
- [ ] Create integration tests for WebSocket functionality using Spring Test
- [ ] Test Redis operations with TestContainers
- [ ] Add end-to-end API testing with TestRestTemplate
- [ ] Test timer tick mechanism with real-time scenarios
- [ ] Create load testing scenarios for concurrent rooms and users
- [ ] Test error scenarios and edge cases

### MILESTONE 7: Performance and Deployment (Week 7)
#### Task 7.1: Performance Optimization
- [ ] Optimize timer tick performance using Spring's thread pools
- [ ] Implement Redis connection pooling and caching strategies
- [ ] Add Spring Boot caching with @Cacheable annotations
- [ ] Optimize WebSocket event broadcasting performance
- [ ] Add performance monitoring using Spring Boot Actuator metrics
- [ ] Create custom metrics for business logic monitoring

#### Task 7.2: Error Handling and Validation
- [ ] Add comprehensive error handling with custom exceptions
- [ ] Implement Bean Validation on all Spring Boot DTOs
- [ ] Create user-friendly error messages with proper HTTP status codes
- [ ] Handle network disconnections gracefully with retry logic
- [ ] Add custom exception classes and @ExceptionHandler methods
- [ ] Implement proper logging with structured logging format

#### Task 7.3: Deployment Configuration
- [ ] Create Dockerfile for Spring Boot application
- [ ] Set up CI/CD pipeline configuration
- [ ] Configure Spring profiles for different environments
- [ ] Set up monitoring with Spring Boot Actuator and Prometheus
- [ ] Create backup and recovery procedures for Redis data
- [ ] Configure SSL certificates and security headers

## Success Metrics
- Timer sync latency: <100ms across all connected devices
- Room creation time: <2 seconds
- Support for 50+ concurrent rooms
- Support for 20+ viewers per room
- 99.9% uptime during events
- All timer operations are real-time synchronized
- Rooms automatically clean up after 24 hours
- WebSocket connections handle reconnection automatically

## Testing Strategy

- Unit tests for service classes using JUnit 5
- Integration tests with TestContainers for Redis
- WebSocket testing with Spring Test framework
- API testing with MockMvc

## Monitoring and Health

- Spring Boot Actuator endpoints at `/actuator/health`
- Custom metrics for room and timer statistics
- OpenAPI documentation at `/swagger-ui.html`
- Application runs on port 3001 by default