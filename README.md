# GymMonitor

GymMonitor is a distributed microservices system that simulates access control for a gym, allowing users to check in and check out while providing a real-time view of how many people are currently inside.

The architecture follows **Event-Driven** and **CQRS** principles. The **AccessControl** service acts as the source of truth, handling check-in/check-out operations, enforcing business rules, and publishing events to RabbitMQ. The **PresenceService** consumes those events to maintain a real-time projection of the current gym state, storing it in Redis for fast access. This data is exposed via REST and pushed to clients over **WebSocket (STOMP)**, enabling instant frontend updates without polling. A dedicated **UserService** handles registration and authentication, issuing JWT tokens with role-based access control (Admin, Employee, Student). An **API Gateway** sits in front of all services as the single entry point, centralizing CORS and JWT validation.

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 17 · Spring Boot 3.5 · Maven multi-module |
| Gateway | Spring Cloud Gateway 2025.0.x (WebFlux) |
| Frontend | Angular 21 · Tailwind CSS 4 · SSR |
| Database | PostgreSQL 16 (UserService, AccessControl) · Redis (PresenceService) |
| Messaging | RabbitMQ |
| Auth | JWT stateless · Spring Security |
| Scripts | Python 3.12 (seed + simulator) |

## Services

| Service | Internal Port | Responsibility |
|---|---|---|
| ApiGateway | 8080 ← único exposto | Routing, CORS, JWT validation |
| UserService | 8082 (internal) | Registration, login, JWT issuance |
| AccessControl | 8081 (internal) | Check-in / check-out, event publishing |
| PresenceService | 8083 (internal) | Real-time presence projection (REST + WebSocket) |
| Frontend | 4200 | Angular UI |

## Running with Docker

```bash
docker compose up --build
```

All services, databases, RabbitMQ, Redis, and demo scripts start automatically. No local JDK or Node.js required.

| URL | Description |
|---|---|
| http://localhost:4200 | Frontend |
| http://localhost:8080 | API Gateway (single external entry point) |
| http://localhost:15672 | RabbitMQ Management (guest/guest) |

> Microservice ports (8081, 8082, 8083) are **not** exposed on the host — all traffic goes through the gateway.

## Demo Data

On first startup, the **seed** container automatically populates the database:

| Type | Count |
|---|---|
| Admin | 1 |
| Funcionários (employees) | 10 |
| Alunos (students) | 2000 |

The **simulator** container runs continuously after seeding, making realistic checkin/checkout calls with time-of-day variation (busier during 6–9h and 17–20h).

## Credentials

### Admin

| Field | Value |
|---|---|
| Email | `GymMonitor@gmail.com` |
| Password | `GymAdmin@123` |

### Funcionários (employees)

| Field | Pattern | Example |
|---|---|---|
| Email | `funcionarioNN@gymmonitor.com` | `funcionario01@gymmonitor.com` |
| Password | `FuncNN@123` | `Func01@123` |

Where `NN` is a zero-padded number from `01` to `10`.

### Alunos (students)

| Field | Pattern | Example |
|---|---|---|
| Email | `alunoNNNN@gymmonitor.com` | `aluno0001@gymmonitor.com` |
| Password | `AlunoNNNN@` | `Aluno0001@` |

Where `NNNN` is a zero-padded number from `0001` to `2000`.

## Frontend

The Angular 21 frontend communicates exclusively with the API Gateway on port 8080. Features:

- **Login page** — role toggle (Funcionário / Admin), JWT stored in `localStorage`
- **Main page** — navbar with theme toggle (dark/light), logout, and "+ Cadastrar" button
- **Registration modal** — Admin sees tabs for Aluno and Funcionário; Funcionário sees only Aluno form
- **404 page** — shown for unknown routes and unauthorized access
- **Theme** — dark by default, persisted in `localStorage`
- **Protected routes** — `authGuard` blocks unauthenticated access; `loggedInGuard` redirects already-logged-in users away from `/login`

## Architecture Notes

### API Gateway

Spring Cloud Gateway (reactive/WebFlux) is the sole externally-exposed service:
- Routes `/api/auth/**` → UserService, `/api/access/**` → AccessControl, `/api/presence/**` → PresenceService
- `StripPrefix` removes `/api` before forwarding
- Global JWT filter validates tokens on all non-public routes
- CORS configured centrally via `CorsWebFilter`
- All microservices communicate over the internal `gymmonitor-net` Docker network using service names

### Database

UserService and AccessControl use separate PostgreSQL 16 instances (`gymmonitor_users` and `gymmonitor_access`). Schema is managed via Docker init SQL scripts. Tests use H2 in-memory — no PostgreSQL required to run `./mvnw test`.

The `sessoes_acesso` table uses a partial unique index (`WHERE saida_em IS NULL`) to enforce one open session per user at the database level.
