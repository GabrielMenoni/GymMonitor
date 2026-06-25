# GymMonitor

GymMonitor é um sistema distribuído de controle de acesso para academias. Alunos e funcionários fazem check-in e check-out, e um painel em tempo real exibe quantas pessoas estão presentes no momento.

## Arquitetura

O sistema é composto por quatro microserviços Java e um frontend Angular, todos comunicando-se internamente através de um API Gateway:

```
                    ┌─────────────────────────────────────────────┐
                    │             Ponto de entrada único           │
                    │   Docker: localhost:8080                     │
                    │   Minikube: k8s.local (Ingress nginx)        │
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

| Serviço | Responsabilidade |
|---|---|
| **ApiGateway** | Único ponto de entrada externo — roteamento, CORS e validação JWT |
| **UserService** | Cadastro e autenticação de usuários, emissão de tokens JWT |
| **AccessControl** | Registro de check-in / check-out, publica eventos no RabbitMQ |
| **PresenceService** | Mantém o estado de presença em tempo real via Redis e WebSocket |
| **Frontend** | Interface Angular com dashboard de presença e gráfico histórico |

**Stack:** Java 17 · Spring Boot 3.5 · Angular 21 · PostgreSQL 16 · Redis · RabbitMQ · Python 3.12

## Credenciais

Criadas automaticamente pelo seed na primeira inicialização.

### Admin

| Campo | Valor |
|---|---|
| Email | `GymMonitor@gmail.com` |
| Senha | `GymAdmin@123` |

### Funcionários

| Campo | Padrão | Exemplo |
|---|---|---|
| Email | `funcionarioNN@gymmonitor.com` | `funcionario01@gymmonitor.com` |
| Senha | `FuncNN@123` | `Func01@123` |

`NN` vai de `01` a `10`.

### Alunos

| Campo | Padrão | Exemplo |
|---|---|---|
| Email | `alunoNNNN@gymmonitor.com` | `aluno0001@gymmonitor.com` |
| Senha | `AlunoNNNN@` | `Aluno0001@` |

`NNNN` vai de `0001` a `2000`.

---

## Utilizando com Docker

Não requer JDK, Node.js ou Python instalados — tudo roda via imagens do DockerHub (`gmenoni/gymmonitor-*`).

### Subir

```bash
docker compose up
```

### Subir em background

```bash
docker compose up -d
```

### Parar

```bash
docker compose down
```

### Parar e remover todos os dados (bancos, filas)

```bash
docker compose down -v
```

### URLs

| URL | Descrição |
|---|---|
| http://localhost:4200 | Frontend |
| http://localhost:8080 | API Gateway |
| http://localhost:15672 | RabbitMQ Management (`guest` / `guest`) |

> As portas internas dos microserviços (8081, 8082, 8083) não são expostas — todo tráfego externo passa pelo gateway.

### Dados de teste (seed)

O admin padrão é criado automaticamente pelo UserService na inicialização. Para popular funcionários e alunos, execute o seed após os containers subirem:

```bash
docker compose run --rm seed
```

O seed é **idempotente** — detecta se os dados já existem e não faz nada em execuções repetidas.

| Tipo | Quantidade |
|---|---|
| Admin | 1 (criado automaticamente) |
| Funcionários | 10 |
| Alunos | 2000 |

### Simulador de tráfego

Simula checkins e checkouts contínuos para gerar dados no dashboard. Execute em um terminal separado com os containers rodando:

```bash
cd scripts/simulator
pip install -r requirements.txt
python simulator.py
```

| Variável | Padrão | Descrição |
|---|---|---|
| `GATEWAY_URL` | `http://localhost:8080/api` | URL base da API |
| `TICK_SECONDS` | `10` | Intervalo entre ciclos (segundos) |
| `MAX_SIMULTANEOUS` | `80` | Máximo de usuários presentes simultaneamente |
| `CHECKINS_PER_TICK` | `5` | Checkins tentados por ciclo |
| `CHECKOUTS_PER_TICK` | `4` | Checkouts tentados por ciclo |

Exemplo — simular horário de pico:

```bash
TICK_SECONDS=5 MAX_SIMULTANEOUS=150 CHECKINS_PER_TICK=15 CHECKOUTS_PER_TICK=5 python simulator.py
```

`Ctrl+C` para parar.

---

## Utilizando com Minikube

O Helm Chart em `helm/gymmonitor/` implanta toda a aplicação no Minikube com um único comando.

### Pré-requisitos

- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm 3](https://helm.sh/docs/intro/install/)

### Build e exportação das imagens

O script `scripts/build-and-load.sh` builda todas as imagens e as carrega diretamente no Minikube (sem necessidade de DockerHub):

```bash
# Executar a partir da raiz do repositório
bash scripts/build-and-load.sh
```

Opções disponíveis:

| Opção | Efeito |
|---|---|
| `--skip-build` | Pula o `docker build` (carrega imagens já existentes localmente) |
| `--skip-load` | Pula o `minikube image load` (apenas builda) |

### Deploy

```bash
# 1. Iniciar o Minikube com recursos suficientes
minikube start --cpus=4 --memory=6144

# 2. Habilitar o Ingress controller nginx
minikube addons enable ingress

# 3. Adicionar k8s.local ao /etc/hosts
echo "$(minikube ip)  k8s.local" | sudo tee -a /etc/hosts

# 4. Instalar o chart — sobe tudo e executa o seed automaticamente
helm install gymmonitor ./helm/gymmonitor

# 5. Aguardar os pods ficarem prontos (3–5 min)
kubectl get pods --watch
```

Quando todos os pods estiverem `1/1 Running` e o Job `gymmonitor-seed` aparecer como `Completed`, a aplicação está pronta.

### URLs

| URL | Descrição |
|---|---|
| http://k8s.local | Frontend |
| http://k8s.local/api/auth/admin/login | Login de admin (POST) |

### Parar e retomar

```bash
# Parar o cluster (dados persistem nos PVCs)
minikube stop

# Retomar
minikube start
```

### Desinstalar

```bash
# Remove o chart (mantém os PVCs com os dados)
helm uninstall gymmonitor

# Remove também os dados persistentes
kubectl delete pvc postgres-users-pvc postgres-access-pvc redis-pvc rabbitmq-pvc

# Ou destrói o cluster inteiro
minikube delete
```

### Simulador de tráfego

Com o cluster rodando, execute o simulador localmente apontando para `k8s.local`:

```bash
cd scripts/simulator
pip install -r requirements.txt
GATEWAY_URL=http://k8s.local/api python simulator.py
```

### Artefatos Kubernetes

| Artefato | Qtd | Função |
|---|---|---|
| `Deployment` | 9 | Um por serviço (infra + microserviços + frontend) |
| `Service` (ClusterIP) | 9 | Comunicação interna entre pods |
| `PersistentVolumeClaim` | 4 | Persistência para PostgreSQL (×2), Redis e RabbitMQ |
| `ConfigMap` | 2 | Scripts SQL de inicialização dos bancos |
| `Secret` | 1 | JWT secret e senhas do PostgreSQL |
| `Job` | 1 | Seed idempotente executado uma vez no pós-install |
| `Ingress` | 1 | Roteamento externo via nginx em `k8s.local` |