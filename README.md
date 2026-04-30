# GymMonitor

GymMonitor is a distributed microservices system that simulates access control for a gym, allowing users to check in and check out while providing a real-time view of how many people are currently inside.

The architecture follows **Event-Driven** and **CQRS** principles. The **AccessControl** service acts as the source of truth, handling check-in/check-out operations, enforcing business rules, and publishing events to RabbitMQ. The **PresenceService** consumes those events to maintain a real-time projection of the current gym state, storing it in Redis for fast access. This data is exposed via REST and pushed to clients over **WebSocket (STOMP)**, enabling instant frontend updates without polling. A dedicated **UserService** handles registration and authentication, issuing JWT tokens with role-based access control (Admin, Student, Employee).

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 17 · Spring Boot 3 · Maven multi-module |
| Frontend | Angular 21 · SSR |
| Messaging | RabbitMQ |
| Cache / State | Redis |
| Auth | JWT (stateless) |

## Services

| Service | Port | Responsibility |
|---|---|---|
| UserService | 8082 | Registration, login, JWT issuance |
| AccessControl | 8081 | Check-in / check-out, event publishing |
| PresenceService | 8083 | Real-time presence projection (REST + WebSocket) |
| Frontend | 4200 | Angular UI |

## Running with Docker

```bash
docker compose up --build
```

All services, RabbitMQ, and Redis are started automatically. No local JDK or Node.js required.

| URL | Description |
|---|---|
| http://localhost:4200 | Frontend |
| http://localhost:8082 | UserService API |
| http://localhost:8081 | AccessControl API |
| http://localhost:8083 | PresenceService API |
| http://localhost:15672 | RabbitMQ Management (guest/guest) |

### Default admin credentials

| Field | Value |
|---|---|
| Email | GymMonitor@gmail.com |
| Password | GymAdmin@123 |
