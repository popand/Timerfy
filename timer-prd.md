# Product Requirements Document (PRD)
# Timerfy - Distributed Timer System

## Project Overview

### Product Name
Timerfy - Distributed Countdown Timer System

### Product Vision
A free, web-based distributed countdown timer system with a decoupled client-server architecture. The server provides RESTful APIs and WebSocket connections, while the client is a standalone React application that can be deployed independently.

### Architecture Philosophy
- **Separation of Concerns**: Client and server are completely independent applications
- **API-First Design**: All functionality accessible via well-defined REST endpoints
- **Real-time Communication**: WebSocket events for live timer updates
- **Stateless Server**: All state managed in Redis, enabling horizontal scaling
- **Deployable Anywhere**: Client can be hosted on CDN, server on any cloud provider

## Technical Architecture

### Separated Project Structure
```
timerfy-system/
├── timerfy-server/            # Independent Spring Boot API server
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/timerfy/
│   │   │   │       ├── controller/     # REST Controllers
│   │   │   │       ├── service/        # Business Logic Services
│   │   │   │       ├── model/          # Entity Models
│   │   │   │       ├── dto/            # Data Transfer Objects
│   │   │   │       ├── config/         # Spring Configuration
│   │   │   │       ├── websocket/      # WebSocket Handlers
│   │   │   │       └── exception/      # Exception Handlers
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── static/
│   │   └── test/
│   ├── pom.xml (or build.gradle)
│   ├── Dockerfile
│   └── README.md
├── timerfy-client/            # Independent React application
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── hooks/
│   │   ├── services/         # API client services
│   │   ├── types/
│   │   └── utils/
│   ├── package.json
│   ├── vite.config.ts
│   ├── Dockerfile
│   └── README.md
├── shared-types/              # Shared TypeScript definitions
│   ├── index.ts
│   └── package.json
└── docker-compose.yml         # For local development only
```

### Technology Stack

#### Server (timer-server)
- **Runtime**: Java 17+ with Spring Boot 3.x
- **Framework**: Spring Boot for REST API with Spring MVC
- **Real-time**: Spring WebSocket with STOMP protocol
- **Database**: Redis for session storage and pub/sub via Spring Data Redis
- **Validation**: Spring Boot Validation with Bean Validation (JSR-303)
- **Documentation**: Spring Doc OpenAPI (Swagger) for API docs
- **Build Tool**: Maven or Gradle
- **Additional**: Spring Boot Actuator for health checks and monitoring

#### Client (timer-client)
- **Framework**: React 18 with TypeScript
- **Styling**: Tailwind CSS
- **HTTP Client**: Axios for API calls
- **WebSocket**: Socket.io-client
- **State Management**: React Query + Context API
- **Build Tool**: Vite

#### Shared Types Package
- **TypeScript definitions** for client-side types
- **Java POJOs/DTOs** for server-side data models
- **JSON Schema** for API contract validation

## API Design Specification

### Base URL Structure
```
Production: https://api.timerfy.io
Development: http://localhost:3001
WebSocket: wss://api.timerfy.io (or ws://localhost:3001)
```

### Authentication
- **No authentication required** for MVP
- **Room-based access control** via room IDs
- **Role-based permissions** (controller vs viewer)

### REST API Endpoints

#### 1. Room Management

##### POST /api/v1/rooms
Create a new timer room.

**Request Body**: None required
```json
{}
```

**Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "id": "ABC123",
    "created": "2025-07-09T10:30:00Z",
    "lastActivity": "2025-07-09T10:30:00Z",
    "expiresAt": "2025-07-10T10:30:00Z",
    "timers": [],
    "messages": [],
    "settings": {
      "maxTimers": 10,
      "autoCleanup": true,
      "allowViewerMessages": false
    },
    "stats": {
      "connectedUsers": 0,
      "totalControllers": 0,
      "totalViewers": 0
    }
  }
}
```

##### GET /api/v1/rooms/:roomId
Get room details and current state.

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": "ABC123",
    "created": "2025-07-09T10:30:00Z",
    "lastActivity": "2025-07-09T10:35:00Z",
    "expiresAt": "2025-07-10T10:30:00Z",
    "timers": [
      {
        "id": "timer-1",
        "name": "Presentation Timer",
        "duration": 1800,
        "currentTime": 1654,
        "state": "running",
        "type": "countdown",
        "createdAt": "2025-07-09T10:32:00Z",
        "startedAt": "2025-07-09T10:33:00Z",
        "settings": {
          "warningTime": 300,
          "criticalTime": 60,
          "autoReset": false
        }
      }
    ],
    "messages": [
      {
        "id": "msg-1",
        "text": "Welcome to the presentation",
        "visible": true,
        "timestamp": "2025-07-09T10:34:00Z",
        "color": "blue",
        "priority": "normal"
      }
    ],
    "settings": {
      "maxTimers": 10,
      "autoCleanup": true,
      "allowViewerMessages": false
    },
    "stats": {
      "connectedUsers": 5,
      "totalControllers": 1,
      "totalViewers": 4
    }
  }
}
```

##### DELETE /api/v1/rooms/:roomId
Delete a room (controller only).

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Room deleted successfully"
}
```

##### GET /api/v1/rooms/:roomId/status
Get lightweight room status (for health checks).

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "exists": true,
    "active": true,
    "connectedUsers": 5,
    "lastActivity": "2025-07-09T10:35:00Z"
  }
}
```

#### 2. Timer Management

##### POST /api/v1/rooms/:roomId/timers
Create a new timer in the room.

**Request Body**:
```json
{
  "name": "Break Timer",
  "duration": 900,
  "type": "countdown",
  "settings": {
    "warningTime": 180,
    "criticalTime": 60,
    "autoReset": false
  }
}
```

**Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "id": "timer-2",
    "name": "Break Timer",
    "duration": 900,
    "currentTime": 900,
    "state": "stopped",
    "type": "countdown",
    "createdAt": "2025-07-09T10:40:00Z",
    "settings": {
      "warningTime": 180,
      "criticalTime": 60,
      "autoReset": false
    }
  }
}
```

##### PUT /api/v1/rooms/:roomId/timers/:timerId
Update timer configuration.

**Request Body**:
```json
{
  "name": "Updated Timer Name",
  "duration": 1200,
  "settings": {
    "warningTime": 240,
    "criticalTime": 60,
    "autoReset": true
  }
}
```

##### DELETE /api/v1/rooms/:roomId/timers/:timerId
Delete a timer.

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Timer deleted successfully"
}
```

#### 3. Timer Control

##### POST /api/v1/rooms/:roomId/timers/:timerId/start
Start a timer.

**Request Body** (optional):
```json
{
  "startTime": "2025-07-09T10:45:00Z"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": "timer-1",
    "state": "running",
    "startedAt": "2025-07-09T10:45:00Z",
    "currentTime": 1800
  }
}
```

##### POST /api/v1/rooms/:roomId/timers/:timerId/stop
Stop a timer.

##### POST /api/v1/rooms/:roomId/timers/:timerId/pause
Pause a timer.

##### POST /api/v1/rooms/:roomId/timers/:timerId/reset
Reset timer to original duration.

**Request Body** (optional):
```json
{
  "newDuration": 1500
}
```

##### POST /api/v1/rooms/:roomId/timers/:timerId/adjust
Adjust timer by adding/subtracting time.

**Request Body**:
```json
{
  "adjustment": 300
}
```

#### 4. Message Management

##### POST /api/v1/rooms/:roomId/messages
Create a new message.

**Request Body**:
```json
{
  "text": "5 minutes remaining",
  "color": "orange",
  "priority": "high",
  "autoShow": true,
  "duration": 10000
}
```

**Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "id": "msg-2",
    "text": "5 minutes remaining",
    "visible": true,
    "timestamp": "2025-07-09T10:50:00Z",
    "color": "orange",
    "priority": "high",
    "autoHideAt": "2025-07-09T10:50:10Z"
  }
}
```

##### PUT /api/v1/rooms/:roomId/messages/:messageId
Update message content or visibility.

**Request Body**:
```json
{
  "text": "Updated message text",
  "visible": false,
  "color": "red"
}
```

##### DELETE /api/v1/rooms/:roomId/messages/:messageId
Delete a message.

##### POST /api/v1/rooms/:roomId/messages/:messageId/show
Show a hidden message.

##### POST /api/v1/rooms/:roomId/messages/:messageId/hide
Hide a visible message.

#### 5. Room Settings

##### PUT /api/v1/rooms/:roomId/settings
Update room settings.

**Request Body**:
```json
{
  "maxTimers": 15,
  "allowViewerMessages": true,
  "theme": {
    "primaryColor": "#3B82F6",
    "backgroundColor": "#000000",
    "fontFamily": "monospace"
  }
}
```

### WebSocket Events

#### Connection Events

##### Client → Server

**STOMP Connect with Headers**
```
CONNECT
destination: /app/room/{roomId}
role: controller
client-info: {"userAgent": "Mozilla/5.0...", "screenResolution": "1920x1080"}
```

**Subscribe to Room Updates**
```
SUBSCRIBE
destination: /topic/room/{roomId}
```

##### Server → Client

**Room Joined Confirmation**
```json
{
  "type": "ROOM_JOINED",
  "success": true,
  "roomData": {
    "id": "ABC123",
    "timers": [],
    "messages": []
  },
  "clientId": "client-uuid",
  "connectedUsers": 3
}
```

**User Connection Updates**
```json
{
  "type": "USER_JOINED",
  "connectedUsers": 4,
  "userCount": {
    "controllers": 1,
    "viewers": 3
  }
}
```

#### Timer Events

##### Server → All Clients

**Timer State Updates**
```json
{
  "type": "TIMER_UPDATED",
  "timerId": "timer-1",
  "currentTime": 1543,
  "state": "RUNNING",
  "timestamp": "2025-07-09T10:51:00Z"
}
```

**Timer Control Events**
```json
{
  "type": "TIMER_STARTED",
  "timerId": "timer-1",
  "startedAt": "2025-07-09T10:51:00Z",
  "currentTime": 1800
}
```

**Timer Warnings**
```json
{
  "type": "TIMER_WARNING",
  "timerId": "timer-1",
  "warningType": "CRITICAL",
  "timeRemaining": 30,
  "timestamp": "2025-07-09T11:20:30Z"
}
```

#### Message Events

**Message Operations**
```json
{
  "type": "MESSAGE_CREATED",
  "messageId": "msg-1",
  "message": {
    "id": "msg-1",
    "text": "Sample message",
    "visible": true,
    "timestamp": "2025-07-09T10:52:00Z"
  }
}
```

#### Error Events

**Error Notifications**
```json
{
  "type": "ERROR",
  "code": "TIMER_NOT_FOUND",
  "message": "Timer with ID timer-999 not found",
  "timestamp": "2025-07-09T10:52:00Z",
  "details": {
    "timerId": "timer-999",
    "roomId": "ABC123"
  }
}
```

### Error Response Format

All API errors follow this structure:

```json
{
  "success": false,
  "error": {
    "code": "ROOM_NOT_FOUND",
    "message": "Room with ID ABC123 not found or has expired",
    "timestamp": "2025-07-09T10:52:00Z",
    "details": {
      "roomId": "ABC123",
      "suggestion": "Please check the room ID or create a new room"
    }
  }
}
```

#### Common Error Codes
- `ROOM_NOT_FOUND` - Room doesn't exist or expired
- `TIMER_NOT_FOUND` - Timer doesn't exist in room
- `MESSAGE_NOT_FOUND` - Message doesn't exist
- `VALIDATION_ERROR` - Invalid input data
- `RATE_LIMIT_EXCEEDED` - Too many requests
- `UNAUTHORIZED` - Invalid permissions for action
- `ROOM_FULL` - Maximum users reached
- `TIMER_LIMIT_REACHED` - Maximum timers in room

## Development Milestones

# MILESTONE 1: Server Foundation (Week 1)

## Task 1.1: Spring Boot Project Setup
### Subtasks:
- [ ] Initialize Spring Boot project with Java 17+ using Spring Initializr
- [ ] Configure Maven/Gradle with required dependencies (Web, WebSocket, Data Redis, Validation, Actuator)
- [ ] Set up Redis connection using Spring Data Redis
- [ ] Configure CORS settings for cross-origin requests
- [ ] Set up application.yml with environment-specific configurations
- [ ] Create basic project structure with packages (controller, service, model, dto, config)
- [ ] Configure Spring Boot Actuator for health checks

## Task 1.2: Core Spring Boot Architecture
### Subtasks:
- [ ] Create Spring Security configuration (if needed) or disable for MVP
- [ ] Set up WebSocket configuration with STOMP protocol
- [ ] Configure Redis as session store and for pub/sub messaging
- [ ] Create global exception handler using @ControllerAdvice
- [ ] Set up request/response logging with Spring AOP
- [ ] Configure Jackson for JSON serialization/deserialization
- [ ] Create health check endpoints using Actuator

## Task 1.3: Data Models and DTOs
### Subtasks:
- [ ] Create Room entity/model class with JPA annotations (for potential future DB use)
- [ ] Create Timer entity/model class with validation annotations
- [ ] Create Message entity/model class
- [ ] Define DTOs for API requests and responses
- [ ] Create enum classes for TimerState, TimerType, MessagePriority, UserRole
- [ ] Set up Bean Validation annotations on DTOs
- [ ] Configure JSON serialization settings for date/time handling

# MILESTONE 2: Server API Implementation (Week 2)

## Task 2.1: Room Management Service
### Subtasks:
- [ ] Create RoomService class with @Service annotation
- [ ] Implement room CRUD operations using RedisTemplate
- [ ] Create room ID generation utility (6-character alphanumeric)
- [ ] Add room expiration logic using Redis TTL
- [ ] Implement room validation using Bean Validation
- [ ] Add room statistics tracking and updates
- [ ] Create scheduled cleanup service using @Scheduled

## Task 2.2: Timer Management Service
### Subtasks:
- [ ] Create TimerService class for timer operations
- [ ] Implement timer state management with thread-safe operations
- [ ] Add timer tick mechanism using Spring's @Async and ScheduledExecutorService
- [ ] Create timer validation logic with custom validators
- [ ] Add timer event publishing using Spring's ApplicationEventPublisher
- [ ] Implement timer persistence using Redis operations
- [ ] Create timer warning/critical time detection

## Task 2.3: REST API Controllers Implementation
### Subtasks:
- [ ] Create RoomController with @RestController annotation
- [ ] Implement TimerController for timer management endpoints
- [ ] Add MessageController for message operations
- [ ] Create request/response DTOs with validation annotations
- [ ] Add global exception handling with @ControllerAdvice
- [ ] Implement rate limiting using Spring AOP or interceptors
- [ ] Configure OpenAPI documentation with SpringDoc

# MILESTONE 3: WebSocket Implementation (Week 3)

## Task 3.1: WebSocket Configuration and Handler
### Subtasks:
- [ ] Create WebSocket configuration class with @EnableWebSocketMessageBroker
- [ ] Configure STOMP endpoints and message broker
- [ ] Create WebSocketHandler for connection management
- [ ] Implement room joining/leaving logic with STOMP subscriptions
- [ ] Add connection state management using WebSocket sessions
- [ ] Create heartbeat mechanism using STOMP heartbeat frames
- [ ] Implement user role management and authorization

## Task 3.2: Timer Event Broadcasting
### Subtasks:
- [ ] Create TimerEventListener using @EventListener
- [ ] Implement real-time timer updates using SimpMessagingTemplate
- [ ] Add timer state change notifications to WebSocket subscribers
- [ ] Create timer warning/critical alerts broadcasting
- [ ] Add timer completion events with sound notification support
- [ ] Implement event rate limiting and message queuing
- [ ] Create timer event DTOs for WebSocket payloads

## Task 3.3: Message Event System
### Subtasks:
- [ ] Create MessageEventListener for message broadcasting
- [ ] Implement message visibility events using WebSocket
- [ ] Add message creation/update/delete event broadcasting
- [ ] Create message priority handling and routing
- [ ] Add auto-hide message functionality with scheduled tasks
- [ ] Implement message queue management for high-priority messages
- [ ] Create message event DTOs and validation

# MILESTONE 4: Client Foundation (Week 4)

## Task 4.1: Client Project Setup
### Subtasks:
- [ ] Initialize React project with Vite and TypeScript
- [ ] Configure Tailwind CSS for styling
- [ ] Set up React Router for navigation
- [ ] Install and configure Axios for HTTP requests
- [ ] Set up STOMP.js for WebSocket communication with Spring Boot
- [ ] Configure environment variables for API endpoints
- [ ] Create project structure and base components

## Task 4.2: API Client Service
### Subtasks:
- [ ] Create centralized API client with Axios
- [ ] Implement room management API calls
- [ ] Add timer control API calls
- [ ] Create message management API calls
- [ ] Add error handling and retry logic
- [ ] Implement response type safety with TypeScript interfaces
- [ ] Configure request/response interceptors

## Task 4.3: WebSocket Service
### Subtasks:
- [ ] Create WebSocket service using STOMP.js client
- [ ] Implement connection management with Spring Boot STOMP endpoints
- [ ] Add STOMP subscription management for room-specific topics
- [ ] Create event listeners for timer and message updates
- [ ] Add reconnection logic with exponential backoff
- [ ] Implement heartbeat mechanism compatible with STOMP
- [ ] Create typed event handlers for Spring Boot WebSocket messages

# MILESTONE 5: Client UI Implementation (Week 5-6)

## Task 5.1: React Context for State Management
### Subtasks:
- [ ] Create Room context for managing room state
- [ ] Implement Timer context for timer-specific state
- [ ] Add WebSocket context for connection management
- [ ] Create combined app context provider
- [ ] Add state synchronization logic
- [ ] Implement optimistic updates

## Task 5.2: Controller Interface
### Subtasks:
- [ ] Create controller dashboard layout
- [ ] Build timer management components
- [ ] Add message management interface
- [ ] Implement timer controls
- [ ] Add room information display
- [ ] Create responsive design

## Task 5.3: Viewer Interface
### Subtasks:
- [ ] Create fullscreen viewer display
- [ ] Implement responsive timer display
- [ ] Add message overlay system
- [ ] Create connection status indicators
- [ ] Add timer state visual effects
- [ ] Implement auto-scaling text

## Task 5.4: Routing and Navigation
### Subtasks:
- [ ] Set up React Router configuration
- [ ] Create protected route components
- [ ] Add navigation between different views
- [ ] Implement room parameter handling
- [ ] Add 404 and error pages
- [ ] Create loading states

# MILESTONE 6: Message System (Week 6)

## Task 6.1: Message Management Service
### Subtasks:
- [ ] Create MessageService class with @Service annotation
- [ ] Add message API endpoints in MessageController
- [ ] Implement message WebSocket events using ApplicationEventPublisher
- [ ] Add message validation and sanitization using Bean Validation
- [ ] Create message auto-hide functionality using @Scheduled tasks
- [ ] Add message priority handling and routing logic
- [ ] Implement message persistence using Redis operations

## Task 6.2: Message UI Components
### Subtasks:
- [ ] Create message creation form with validation
- [ ] Build message list component with real-time updates
- [ ] Add message visibility controls integrated with STOMP
- [ ] Create message overlay for viewer with priority styling
- [ ] Add message styling options and color themes
- [ ] Implement message queue management with Spring Boot events

# MILESTONE 7: Polish and Deployment (Week 7-8)

## Task 7.1: Error Handling and Validation
### Subtasks:
- [ ] Add comprehensive error boundaries in React
- [ ] Implement Bean Validation on all Spring Boot DTOs
- [ ] Create user-friendly error messages with i18n support
- [ ] Add loading states throughout React app
- [ ] Handle network disconnections gracefully with retry logic
- [ ] Add custom exception classes and @ExceptionHandler methods

## Task 7.2: Performance Optimization
### Subtasks:
- [ ] Optimize timer tick performance using Spring's thread pools
- [ ] Add React.memo for expensive components
- [ ] Implement Redis connection pooling and caching strategies
- [ ] Add Spring Boot caching with @Cacheable annotations
- [ ] Optimize bundle size with code splitting
- [ ] Add performance monitoring using Spring Boot Actuator metrics

## Task 7.3: Testing
### Subtasks:
- [ ] Write JUnit tests for Spring Boot services and controllers
- [ ] Create integration tests for WebSocket functionality using Spring Test
- [ ] Add React Testing Library tests for UI components
- [ ] Test mobile responsiveness across devices
- [ ] Load testing with JMeter for concurrent rooms and users
- [ ] Test error scenarios and edge cases

## Task 7.4: Deployment Configuration
### Subtasks:
- [ ] Create Dockerfile for Spring Boot application
- [ ] Set up CI/CD pipeline with GitHub Actions or Jenkins
- [ ] Configure Spring profiles for different environments
- [ ] Set up monitoring with Spring Boot Actuator and Prometheus
- [ ] Create backup and recovery procedures for Redis data
- [ ] Configure SSL certificates and security headers

## Task 7.5: Documentation and Launch
### Subtasks:
- [ ] Create comprehensive README with Spring Boot setup instructions
- [ ] Generate API documentation using SpringDoc OpenAPI
- [ ] Create user guide and deployment documentation
- [ ] Set up monitoring dashboard with Grafana
- [ ] Create Docker Compose files for easy deployment
- [ ] Launch production environment on cloud provider

## Success Metrics and Testing Criteria

### Performance Benchmarks
- [ ] Timer sync latency: <100ms across all connected devices
- [ ] Room creation time: <2 seconds
- [ ] Support for 50+ concurrent rooms
- [ ] Support for 20+ viewers per room
- [ ] 99.9% uptime during events

### User Experience Criteria
- [ ] Mobile-responsive design works on all screen sizes
- [ ] Intuitive interface requires no tutorial
- [ ] Room creation and joining takes <30 seconds
- [ ] Visual feedback for all user actions
- [ ] Graceful handling of network interruptions

### Technical Requirements
- [ ] All timer operations are real-time synchronized
- [ ] Rooms automatically clean up after 24 hours
- [ ] WebSocket connections handle reconnection automatically
- [ ] All user inputs are validated and sanitized
- [ ] Error states are handled gracefully

This PRD provides a comprehensive roadmap for building a distributed timer system that matches the core functionality of StageTimer.io while remaining completely free and open-source. The modular approach allows for iterative development and easy testing of individual components.