# Timerfy - Distributed Timer System

<div align="center">

![Timerfy Logo](https://img.shields.io/badge/Timerfy-Distributed%20Timer%20System-blue?style=for-the-badge)

[![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.0+-red?style=flat-square&logo=redis)](https://redis.io/)
[![React](https://img.shields.io/badge/React-18+-blue?style=flat-square&logo=react)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0+-blue?style=flat-square&logo=typescript)](https://www.typescriptlang.org/)

**A free, open-source distributed countdown timer system with real-time synchronization**

[Demo](https://timerfy.io) • [Documentation](#api-documentation) • [Contributing](#contributing) • [Roadmap](#development-roadmap)

</div>

## 🎯 Project Overview

Timerfy is a web-based distributed countdown timer system designed as a free, open-source alternative to commercial timer solutions. Built with a modern decoupled architecture, it provides real-time timer synchronization across multiple devices through RESTful APIs and WebSocket connections.

### ✨ Key Features

- **🚀 Real-time Synchronization**: Sub-100ms latency across all connected devices
- **🏗️ Decoupled Architecture**: Independent client and server applications
- **📱 Multi-device Support**: Works seamlessly across desktop, tablet, and mobile
- **⚡ High Performance**: Supports 50+ concurrent rooms with 20+ viewers each
- **🔄 Auto-cleanup**: Rooms automatically expire after 24 hours
- **🎨 Customizable**: Flexible timer configurations and message system
- **🔌 API-First**: Complete functionality accessible via REST APIs
- **📡 WebSocket Events**: Live updates without polling

### 🎪 Use Cases

- **Conference Presentations**: Professional speaker timing
- **Educational Settings**: Classroom activities and exams
- **Corporate Events**: Meeting time management
- **Sports Events**: Competition timing
- **Broadcasting**: Live show timing systems
- **Workshops**: Training session management

## 🏗️ Architecture Overview

Timerfy follows a **separation of concerns** principle with completely independent applications:

```
timerfy-system/
├── timerfy-server/            # Spring Boot API Server
│   ├── src/main/java/
│   │   └── com/timerfy/
│   │       ├── controller/    # REST Controllers
│   │       ├── service/       # Business Logic
│   │       ├── model/         # Data Models
│   │       ├── dto/           # Data Transfer Objects
│   │       ├── config/        # Configuration
│   │       └── websocket/     # WebSocket Handlers
│   └── pom.xml
├── timerfy-client/            # React Application (Coming Soon)
│   ├── src/
│   │   ├── components/
│   │   ├── services/
│   │   └── types/
│   └── package.json
└── shared-types/              # Shared Type Definitions (Coming Soon)
```

### 🛠️ Technology Stack

#### Backend (timerfy-server) ✅ **IMPLEMENTED**
- **Runtime**: Java 17+ with Spring Boot 3.2+
- **Framework**: Spring MVC for REST APIs
- **WebSockets**: Spring WebSocket with STOMP protocol
- **Database**: Redis for state management and pub/sub
- **Validation**: Bean Validation (JSR-303)
- **Documentation**: SpringDoc OpenAPI (Swagger)
- **Build**: Maven
- **Monitoring**: Spring Boot Actuator

#### Frontend (timerfy-client) 🚧 **PLANNED**
- **Framework**: React 18 with TypeScript
- **Styling**: Tailwind CSS
- **HTTP Client**: Axios
- **WebSocket**: STOMP.js
- **State Management**: React Query + Context API
- **Build**: Vite

#### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Caching**: Redis
- **Monitoring**: Prometheus + Grafana
- **CI/CD**: GitHub Actions

## 📊 Current Implementation Status

### ✅ Milestone 1: Foundation (COMPLETED)
- [x] Spring Boot project setup
- [x] Redis integration
- [x] CORS configuration
- [x] Basic project structure

### ✅ Milestone 2: Data Models & Core Services (COMPLETED)
- [x] **Enums**: TimerState, TimerType, MessagePriority, UserRole
- [x] **Models**: Room, Timer, Message entities with validation
- [x] **DTOs**: Request/response objects with Bean Validation
- [x] **Services**: RoomService, TimerService with thread-safe operations
- [x] **Redis Integration**: Complete persistence layer with TTL
- [x] **Scheduled Tasks**: Background cleanup and maintenance
- [x] **Event System**: ApplicationEventPublisher for WebSocket integration

### 🚧 Milestone 3: REST API Implementation (IN PROGRESS)
- [ ] Global exception handling
- [ ] RoomController, TimerController, MessageController
- [ ] Complete REST API endpoints
- [ ] OpenAPI documentation
- [ ] Rate limiting

### 🗓️ Upcoming Milestones
- **Milestone 4**: WebSocket Implementation
- **Milestone 5**: React Client Foundation
- **Milestone 6**: Client UI Implementation
- **Milestone 7**: Message System
- **Milestone 8**: Polish and Deployment

## 🚀 Quick Start

### Prerequisites

- **Java 17+** ([Download](https://adoptium.net/))
- **Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **Redis 7.0+** ([Download](https://redis.io/download) or use Docker)

### 🏃‍♂️ Running the Server

1. **Clone the repository**
   ```bash
   git clone https://github.com/popand/Timerfy.git
   cd Timerfy
   ```

2. **Start Redis** (using Docker)
   ```bash
   docker run -d -p 6379:6379 --name timerfy-redis redis:7-alpine
   ```

3. **Build and run the server**
   ```bash
   cd timerfy-server
   mvn clean compile
   mvn spring-boot:run
   ```

4. **Verify the server is running**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### 🐳 Docker Setup

```bash
# Start Redis and the application
docker-compose up -d

# Check logs
docker-compose logs -f
```

### 🔧 Configuration

The server can be configured via `application.yml`:

```yaml
server:
  port: 8080

spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

timerfy:
  rooms:
    ttl: 86400  # 24 hours in seconds
    max-timers: 10
    cleanup-interval: 3600  # 1 hour in seconds
```

## 📚 API Documentation

### Base URL
- **Development**: `http://localhost:8080`
- **Production**: `https://api.timerfy.io` (Coming Soon)

### Authentication
- **No authentication required** for MVP
- **Room-based access control** via room IDs
- **Role-based permissions** (controller vs viewer)

### Core Endpoints

#### Room Management
```http
POST   /api/v1/rooms                     # Create new room
GET    /api/v1/rooms/{roomId}           # Get room details
DELETE /api/v1/rooms/{roomId}           # Delete room
GET    /api/v1/rooms/{roomId}/status    # Get room status
```

#### Timer Control
```http
POST   /api/v1/rooms/{roomId}/timers                    # Create timer
PUT    /api/v1/rooms/{roomId}/timers/{timerId}         # Update timer
DELETE /api/v1/rooms/{roomId}/timers/{timerId}         # Delete timer
POST   /api/v1/rooms/{roomId}/timers/{timerId}/start   # Start timer
POST   /api/v1/rooms/{roomId}/timers/{timerId}/stop    # Stop timer
POST   /api/v1/rooms/{roomId}/timers/{timerId}/pause   # Pause timer
POST   /api/v1/rooms/{roomId}/timers/{timerId}/reset   # Reset timer
```

#### WebSocket Connection
```javascript
// STOMP connection to room updates
const client = Stomp.over(new SockJS('/ws'));
client.connect({}, () => {
  client.subscribe('/topic/room/{roomId}', (message) => {
    // Handle real-time updates
  });
});
```

### 📖 Interactive API Documentation

Once the server is running, visit:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

## 🧪 Testing

### Running Tests
```bash
cd timerfy-server

# Run unit tests
mvn test

# Run integration tests
mvn verify

# Generate test coverage report
mvn jacoco:report
```

### Load Testing
```bash
# Test concurrent rooms (requires JMeter)
jmeter -n -t load-tests/room-load-test.jmx
```

### Performance Targets
- ⚡ **Timer sync latency**: <100ms
- 🏠 **Room creation**: <2 seconds
- 👥 **Concurrent rooms**: 50+
- 📺 **Viewers per room**: 20+
- ⏱️ **Uptime**: 99.9%

## 🗺️ Development Roadmap

### Phase 1: Server Foundation ✅
- [x] Spring Boot setup and Redis integration
- [x] Data models and core services
- [ ] Complete REST API implementation

### Phase 2: Real-time Communication 🚧
- [ ] WebSocket configuration and handlers
- [ ] Real-time timer event broadcasting
- [ ] Message system implementation

### Phase 3: React Client 📅
- [ ] React application setup with TypeScript
- [ ] API client services and WebSocket integration
- [ ] Controller and viewer interfaces
- [ ] Responsive design implementation

### Phase 4: Production Ready 📅
- [ ] Comprehensive testing suite
- [ ] Performance optimization
- [ ] CI/CD pipeline setup
- [ ] Production deployment configuration
- [ ] Monitoring and analytics

### Phase 5: Advanced Features 📅
- [ ] User authentication system
- [ ] Room templates and presets
- [ ] Custom themes and branding
- [ ] Analytics dashboard
- [ ] Multi-language support
- [ ] Mobile apps (React Native)

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Code Style
- **Java**: Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- **TypeScript**: Use Prettier with ESLint
- **Commits**: Follow [Conventional Commits](https://conventionalcommits.org/)

### Issues and Bug Reports
Please use our [issue tracker](https://github.com/popand/Timerfy/issues) to report bugs or request features.

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by [StageTimer.io](https://stagetimer.io) - providing a free alternative
- Built with [Spring Boot](https://spring.io/projects/spring-boot) and [React](https://reactjs.org/)
- Redis for fast, reliable state management
- The open-source community for continuous inspiration

## 📞 Support

- 📧 **Email**: support@timerfy.io
- 💬 **Discord**: [Join our community](https://discord.gg/timerfy)
- 🐛 **Issues**: [GitHub Issues](https://github.com/popand/Timerfy/issues)
- 📖 **Documentation**: [docs.timerfy.io](https://docs.timerfy.io)

---

<div align="center">

**Made with ❤️ by the Timerfy Team**

[⭐ Star this repo](https://github.com/popand/Timerfy) • [🐛 Report bug](https://github.com/popand/Timerfy/issues) • [✨ Request feature](https://github.com/popand/Timerfy/issues)

</div> 