# Timerfy Server

Distributed Timer System - Spring Boot Server Implementation

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Redis Server

### Development Setup

1. Start Redis server:
```bash
redis-server
```

2. Run the application:
```bash
mvn spring-boot:run
```

3. Access the application:
- API Base URL: http://localhost:3001
- Swagger UI: http://localhost:3001/swagger-ui.html
- Health Check: http://localhost:3001/actuator/health

## Build Commands

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Package the application
mvn package

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Configuration

The application uses `application.yml` for configuration. Key settings:

- Server port: 3001
- Redis connection details
- CORS settings for client connections
- WebSocket configuration
- Room expiration settings

## API Documentation

Once running, visit http://localhost:3001/swagger-ui.html for interactive API documentation.

## Architecture

The server follows a layered architecture:
- **Controllers**: REST API endpoints
- **Services**: Business logic
- **Models**: Data entities
- **DTOs**: Data transfer objects
- **Config**: Spring configuration
- **WebSocket**: Real-time communication handlers
- **Exception**: Global exception handling