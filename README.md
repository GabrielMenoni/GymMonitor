# GymMonitor

GymMonitor is a distributed access control system for gyms. Students and employees check in and check out, and a real-time dashboard shows how many people are currently inside.

## Architecture

The system is composed of four Java microservices and an Angular frontend, all communicating internally through an API Gateway:

```
                    ┌─────────────────────────────────────────────┐
                    │              Single entry point              │
                    │   Docker:    localhost:8080                  │
                    │   Minikube:  k8s.local (nginx Ingress)       │
                    └──────────────┬──────────────┬───────────────┘
                                   │              │
                       ┌───────────▼──┐    ┌──────▼──────┐
                       │  ApiGateway  │    │  Frontend   │
                       │   :8080      │    │   :4200     │
                       └──┬───┬───┬───┘    └─────────────┘
                          │   │   │
           ┌──────────────┘   │   └──────────────────┐
           │                  │                       │
┌──────────▼──┐    ┌──────────▼──┐    ┌──────────────▼──┐
│ UserService │    │AccessControl│    │PresenceService  │
│   :8082     │    │   :8081     │    │   :8083         │
└──────┬──────┘    └──────┬──────┘    └───────┬─────────┘
       │                  │                   │        │
┌──────▼──────┐    ┌──────▼──────┐    ┌───────▼──┐  ┌──▼────┐
│  PostgreSQL │    │  PostgreSQL │    │ RabbitMQ │  │ Redis │
│   (users)   │    │  (access)   │    │  :5672   │  │ :6379 │
└─────────────┘    └─────────────┘    └──────────┘  └───────┘
```

| Service | Responsibility |
|---|---|
| **ApiGateway** | Single external entry point — routing, CORS and JWT validation |
| **UserService** | User registration and authentication, JWT token issuance |
| **AccessControl** | Check-in / check-out records, publishes events to RabbitMQ |
| **PresenceService** | Maintains real-time presence state via Redis and WebSocket |
| **Frontend** | Angular UI with presence dashboard and history chart |

**Stack:** Java 17 · Spring Boot 3.5 · Angular 21 · PostgreSQL 16 · Redis · RabbitMQ · Python 3.12

## Credentials

Created automatically by the seed on first startup.

### Admin

| Field | Value |
|---|---|
| Email | `GymMonitor@gmail.com` |
| Password | `GymAdmin@123` |

### Employees

| Field | Pattern | Example |
|---|---|---|
| Email | `funcionarioNN@gymmonitor.com` | `funcionario01@gymmonitor.com` |
| Password | `FuncNN@123` | `Func01@123` |

`NN` ranges from `01` to `10`.

### Students

| Field | Pattern | Example |
|---|---|---|
| Email | `alunoNNNN@gymmonitor.com` | `aluno0001@gymmonitor.com` |
| Password | `AlunoNNNN@` | `Aluno0001@` |

`NNNN` ranges from `0001` to `2000`.

---

## Running with Docker

No local JDK, Node.js or Python required — everything runs from DockerHub images (`gmenoni/gymmonitor-*`).

### Start

```bash
docker compose up
```

### Start in background

```bash
docker compose up -d
```

### Stop

```bash
docker compose down
```

### Stop and remove all data (databases, queues)

```bash
docker compose down -v
```

### URLs

| URL | Description |
|---|---|
| http://localhost:4200 | Frontend |
| http://localhost:8080 | API Gateway |
| http://localhost:15672 | RabbitMQ Management (`guest` / `guest`) |

> Internal microservice ports (8081, 8082, 8083) are not exposed — all external traffic goes through the gateway.

### Demo data (seed)

The default admin account is created automatically by the UserService on startup. To populate employees and students, run the seed after the containers are up:

```bash
docker compose run --rm seed
```

The seed is **idempotent** — it detects whether the data already exists and does nothing on repeated runs.

| Type | Count |
|---|---|
| Admin | 1 (created automatically) |
| Employees | 10 |
| Students | 2000 |

### Traffic simulator

Simulates continuous check-ins and check-outs to generate data for the dashboard. Run in a separate terminal with the containers up:

```bash
cd scripts/simulator
pip install -r requirements.txt
python simulator.py
```

| Variable | Default | Description |
|---|---|---|
| `GATEWAY_URL` | `http://localhost:8080/api` | API base URL |
| `TICK_SECONDS` | `10` | Interval between ticks (seconds) |
| `MAX_SIMULTANEOUS` | `80` | Maximum users inside at the same time |
| `CHECKINS_PER_TICK` | `5` | Check-ins attempted per tick |
| `CHECKOUTS_PER_TICK` | `4` | Check-outs attempted per tick |

Example — simulate peak hour:

```bash
TICK_SECONDS=5 MAX_SIMULTANEOUS=150 CHECKINS_PER_TICK=15 CHECKOUTS_PER_TICK=5 python simulator.py
```

Press `Ctrl+C` to stop.

---

## Running with Minikube

The Helm Chart in `helm/gymmonitor/` deploys the entire application to Minikube with a single command.

### Prerequisites

- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm 3](https://helm.sh/docs/intro/install/)

### Building and loading images

The `scripts/build-and-load.sh` script builds all images and loads them directly into Minikube (no DockerHub push required):

```bash
# Run from the repository root
bash scripts/build-and-load.sh
```

Available options:

| Option | Effect |
|---|---|
| `--skip-build` | Skip `docker build` (load already-built local images) |
| `--skip-load` | Skip `minikube image load` (build only) |

### Deploy

```bash
# 1. Start Minikube with enough resources
minikube start --cpus=4 --memory=6144

# 2. Enable the nginx Ingress controller
minikube addons enable ingress

# 3. Add k8s.local to /etc/hosts
echo "$(minikube ip)  k8s.local" | sudo tee -a /etc/hosts

# 4. Install the chart — starts everything and runs the seed automatically
helm install gymmonitor ./helm/gymmonitor

# 5. Wait for all pods to be ready (3–5 min)
kubectl get pods --watch
```

When all pods show `1/1 Running` and the `gymmonitor-seed` Job shows `Completed`, the application is ready.

### URLs

| URL | Description |
|---|---|
| http://k8s.local | Frontend |
| http://k8s.local/api/auth/admin/login | Admin login (POST) |

### Stop and resume

```bash
# Stop the cluster (data persists in PVCs)
minikube stop

# Resume
minikube start
```

### Uninstall

```bash
# Remove the chart (PVCs and data are kept)
helm uninstall gymmonitor

# Also remove persistent data
kubectl delete pvc postgres-users-pvc postgres-access-pvc redis-pvc rabbitmq-pvc

# Or destroy the entire cluster
minikube delete
```

### Traffic simulator

With the cluster running, run the simulator locally pointing to `k8s.local`:

```bash
cd scripts/simulator
pip install -r requirements.txt
GATEWAY_URL=http://k8s.local/api python simulator.py
```

### Kubernetes artifacts

| Artifact | Count | Purpose |
|---|---|---|
| `Deployment` | 9 | One per service (infra + microservices + frontend) |
| `Service` (ClusterIP) | 9 | Internal pod-to-pod communication |
| `PersistentVolumeClaim` | 4 | Persistent storage for PostgreSQL (×2), Redis and RabbitMQ |
| `ConfigMap` | 2 | PostgreSQL initialization SQL scripts |
| `Secret` | 1 | JWT secret and PostgreSQL passwords |
| `Job` | 1 | Idempotent seed run once as a post-install hook |
| `Ingress` | 1 | External routing via nginx at `k8s.local` |

### Test plan

Steps to validate a fresh Minikube deployment.

**1. All pods running:**

```bash
kubectl get pods
```

Expected: all pods `1/1 Running`, Job `gymmonitor-seed` as `Completed`.

**2. Ingress has an address:**

```bash
kubectl get ingress gymmonitor-ingress
```

Expected: `ADDRESS` column shows the Minikube IP.

**3. Admin login:**

```bash
curl -s -X POST http://k8s.local/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email":"GymMonitor@gmail.com","password":"GymAdmin@123"}'
```

Expected: JSON with `token` and `role: "ADMIN"`.

**4. Seed data:**

```bash
# Replace <TOKEN> with the token from the previous step
curl -s http://k8s.local/api/user-service/alunos/count \
  -H "Authorization: Bearer <TOKEN>"
# Expected: {"count":2000}

curl -s http://k8s.local/api/user-service/funcionarios/count \
  -H "Authorization: Bearer <TOKEN>"
# Expected: {"count":10}
```

**5. Employee check-in:**

```bash
TOKEN=$(curl -s -X POST http://k8s.local/api/auth/funcionarios/login \
  -H "Content-Type: application/json" \
  -d '{"email":"funcionario01@gymmonitor.com","password":"Func01@123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s -X POST http://k8s.local/api/access/checkin \
  -H "Authorization: Bearer $TOKEN"
```

Expected: JSON with `sessaoId`, `entradaEm` and `userType: "FUNCIONARIO"`.

**6. Frontend and WebSocket:**

Open `http://k8s.local` in the browser, log in as Admin, and verify the presence counter updates in real time.
