#!/usr/bin/env bash
set -euo pipefail

# Build all GymMonitor Docker images and load them into Minikube.
# Run from the repository root: bash scripts/build-and-load.sh
#
# Options:
#   --skip-build   Skip docker build (only load already-built images)
#   --skip-load    Skip minikube image load (only build)

SKIP_BUILD=false
SKIP_LOAD=false

for arg in "$@"; do
  case $arg in
    --skip-build) SKIP_BUILD=true ;;
    --skip-load)  SKIP_LOAD=true ;;
  esac
done

IMAGES=(
  "gmenoni/gymmonitor-user-service:latest|backend/gymmonitor/services/UserService"
  "gmenoni/gymmonitor-access-control:latest|backend/gymmonitor/services/AccessControl"
  "gmenoni/gymmonitor-presence-service:latest|backend/gymmonitor/services/PresenceService"
  "gmenoni/gymmonitor-api-gateway:latest|backend/gymmonitor/services/ApiGateway"
  "gmenoni/gymmonitor-frontend:latest|frontend"
  "gmenoni/gymmonitor-seed:latest|scripts/seed"
  "gmenoni/gymmonitor-simulator:latest|scripts/simulator"
)

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
ok()   { echo "[$(date '+%H:%M:%S')] ✓ $*"; }
fail() { echo "[$(date '+%H:%M:%S')] ✗ $*" >&2; exit 1; }

# ── Build ────────────────────────────────────────────────────────────────────

if [ "$SKIP_BUILD" = false ]; then
  log "Building ${#IMAGES[@]} images..."
  for entry in "${IMAGES[@]}"; do
    image="${entry%%|*}"
    context="${entry##*|}"
    log "Building $image from $context/"
    docker build -t "$image" "$context/" || fail "Build failed: $image"
    ok "$image"
  done
else
  log "--skip-build: skipping docker build"
fi

# ── Load into Minikube ───────────────────────────────────────────────────────

if [ "$SKIP_LOAD" = false ]; then
  if ! minikube status --format '{{.Host}}' 2>/dev/null | grep -q "Running"; then
    fail "Minikube is not running. Start it first: minikube start --cpus=4 --memory=6144"
  fi

  log "Loading ${#IMAGES[@]} images into Minikube..."
  for entry in "${IMAGES[@]}"; do
    image="${entry%%|*}"
    log "Loading $image"
    minikube image load "$image" || fail "Load failed: $image"
    ok "$image"
  done
else
  log "--skip-load: skipping minikube image load"
fi

echo ""
log "Done. Deploy with: helm install gymmonitor ./helm/gymmonitor"
log "Or upgrade:        helm upgrade gymmonitor ./helm/gymmonitor"
