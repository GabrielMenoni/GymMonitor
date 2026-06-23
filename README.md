# GymMonitor

GymMonitor is a distributed microservices system that simulates access control for a gym, allowing users to check in and check out while providing a real-time view of how many people are currently inside.

The architecture follows **Event-Driven** and **CQRS** principles. The **AccessControl** service acts as the source of truth, handling check-in/check-out operations, enforcing business rules, and publishing events to RabbitMQ. The **PresenceService** consumes those events to maintain a real-time projection of the current gym state, storing it in Redis for fast access. This data is exposed via REST and pushed to clients over **WebSocket (STOMP)**, enabling instant frontend updates without polling. A dedicated **UserService** handles registration and authentication, issuing JWT tokens with role-based access control (Admin, Employee, Student). An **API Gateway** sits in front of all services as the single entry point, centralizing CORS and JWT validation.

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 17 · Spring Boot 3.5 · Maven multi-module |
| Gateway | Spring Cloud Gateway 2025.0.x (WebFlux) |
| Frontend | Angular 21 · Tailwind CSS 4 · Chart.js · SSR |
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

The `docker-compose.yml` uses pre-built images from Docker Hub (`gmenoni/gymmonitor-*`). No local JDK or Node.js required.

### Start everything

```bash
docker compose up
```

### Start in background

```bash
docker compose up -d
```

### Stop all containers

```bash
docker compose down
```

### Stop and remove all data (databases, queues)

```bash
docker compose down -v
```

All services, databases, RabbitMQ, and Redis start automatically.

| URL | Description |
|---|---|
| http://localhost:4200 | Frontend |
| http://localhost:8080 | API Gateway (single external entry point) |
| http://localhost:15672 | RabbitMQ Management (guest/guest) |

> Microservice ports (8081, 8082, 8083) are **not** exposed on the host — all traffic goes through the gateway.

## Demo Data

The default admin account is created automatically on first startup by the UserService. To populate the database with demo employees and students, run the seed script using Docker (no local Python required):

```bash
docker compose run --rm seed
```

The seed is **idempotent** — it checks whether the data already exists before running. Re-running it after the database has been populated is safe and has no effect.

This creates:

| Type | Count |
|---|---|
| Admin | 1 (auto-created by UserService) |
| Funcionários (employees) | 10 |
| Alunos (students) | 2000 |

## Traffic Simulator

The simulator continuously calls the checkin/checkout APIs, generating realistic presence data for the dashboard. Run it using Docker (no local Python required):

```bash
docker compose run --rm simulator
```

Or, if you prefer, run it locally in a separate terminal while containers are up:

```bash
cd scripts/simulator
pip install -r requirements.txt
python simulator.py
```

### Configuration via environment variables

| Variable | Default | Description |
|---|---|---|
| `GATEWAY_URL` | `http://localhost:8080/api` | API Gateway base URL |
| `TICK_SECONDS` | `10` | Interval between ticks (seconds) |
| `MAX_SIMULTANEOUS` | `80` | Max users inside at the same time |
| `CHECKINS_PER_TICK` | `5` | Checkins attempted per tick |
| `CHECKOUTS_PER_TICK` | `4` | Checkouts attempted per tick |

Example — simulate a peak hour with higher volume:

```bash
TICK_SECONDS=5 MAX_SIMULTANEOUS=150 CHECKINS_PER_TICK=15 CHECKOUTS_PER_TICK=5 python simulator.py
```

Press `Ctrl+C` to stop the simulator.

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
