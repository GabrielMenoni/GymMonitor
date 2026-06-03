import os
import sys
import time
import random
import logging
import requests
from datetime import datetime

GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8080/api")
TICK_SECONDS = int(os.getenv("TICK_SECONDS", "10"))
MAX_SIMULTANEOUS = int(os.getenv("MAX_SIMULTANEOUS", "80"))
CHECKINS_PER_TICK = int(os.getenv("CHECKINS_PER_TICK", "5"))
CHECKOUTS_PER_TICK = int(os.getenv("CHECKOUTS_PER_TICK", "4"))
MAX_RETRIES = 40
RETRY_DELAY = 5

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)

NUM_FUNCIONARIOS = 10
NUM_ALUNOS = 2000

TOKEN_TTL_SECONDS = 100 * 60  # 100 minutos (tokens expiram em 120min)

# estado: email -> (token, timestamp_criacao)
tokens_cache: dict[str, tuple[str, float]] = {}
inside: set[str] = set()   # emails com checkin aberto


def wait_for_gateway():
    log.info(f"Aguardando gateway em {GATEWAY_URL}...")
    for attempt in range(MAX_RETRIES):
        try:
            base = GATEWAY_URL[: GATEWAY_URL.rfind("/api")] if "/api" in GATEWAY_URL else GATEWAY_URL
            r = requests.get(
                f"{base}/actuator/health", timeout=5
            )
            if r.status_code == 200:
                log.info("Gateway disponível.")
                return
        except requests.exceptions.ConnectionError:
            pass
        log.info(f"  Tentativa {attempt + 1}/{MAX_RETRIES} — aguardando {RETRY_DELAY}s...")
        time.sleep(RETRY_DELAY)
    log.error("Gateway não ficou disponível.")
    sys.exit(1)


def get_token(email: str, password: str, role: str) -> str | None:
    cached = tokens_cache.get(email)
    if cached:
        token, created_at = cached
        if time.monotonic() - created_at < TOKEN_TTL_SECONDS:
            return token
        # token expirado — remove do cache para forçar novo login
        del tokens_cache[email]

    endpoint = "/auth/funcionarios/login" if role == "FUNCIONARIO" else "/auth/alunos/login"
    try:
        r = requests.post(
            f"{GATEWAY_URL}{endpoint}",
            json={"email": email, "password": password},
            timeout=10,
        )
        if r.status_code == 200:
            token = r.json()["token"]
            tokens_cache[email] = (token, time.monotonic())
            return token
    except requests.exceptions.RequestException as e:
        log.warning(f"Login falhou para {email}: {e}")
    return None


def do_checkin(email: str, token: str) -> bool:
    try:
        r = requests.post(
            f"{GATEWAY_URL}/access/checkin",
            headers={"Authorization": f"Bearer {token}"},
            timeout=10,
        )
        if r.status_code == 201:
            inside.add(email)
            return True
        if r.status_code == 409:
            # já tem checkin aberto — sincronizar estado local
            inside.add(email)
            return False
    except requests.exceptions.RequestException as e:
        log.warning(f"Checkin falhou para {email}: {e}")
    return False


def do_checkout(email: str, token: str) -> bool:
    try:
        r = requests.post(
            f"{GATEWAY_URL}/access/checkout",
            headers={"Authorization": f"Bearer {token}"},
            timeout=10,
        )
        if r.status_code == 200:
            inside.discard(email)
            return True
        if r.status_code == 404:
            inside.discard(email)
            return False
    except requests.exceptions.RequestException as e:
        log.warning(f"Checkout falhou para {email}: {e}")
    return False


def build_user_pool() -> list[tuple[str, str, str]]:
    """Retorna lista de (email, password, role)."""
    users = []
    for i in range(1, NUM_FUNCIONARIOS + 1):
        users.append((
            f"funcionario{i:02d}@gymmonitor.com",
            f"Func{i:02d}@123",
            "FUNCIONARIO",
        ))
    for i in range(1, NUM_ALUNOS + 1):
        users.append((
            f"aluno{i:04d}@gymmonitor.com",
            f"Aluno{i:04d}@",
            "ALUNO",
        ))
    return users


def main():
    wait_for_gateway()

    all_users = build_user_pool()
    log.info(f"Pool: {len(all_users)} usuários ({NUM_FUNCIONARIOS} funcionários + {NUM_ALUNOS} alunos)")
    log.info(f"Tick: {TICK_SECONDS}s | Max simultâneos: {MAX_SIMULTANEOUS} | Checkins/tick: {CHECKINS_PER_TICK} | Checkouts/tick: {CHECKOUTS_PER_TICK}")

    tick = 0
    while True:
        tick += 1
        hour = datetime.now().hour

        # Ajustar intensidade por hora do dia
        if 6 <= hour < 9 or 17 <= hour < 20:
            # horário de pico — mais entradas que saídas
            ci = CHECKINS_PER_TICK + 2
            co = CHECKOUTS_PER_TICK
        elif 22 <= hour or hour < 5:
            # madrugada — academia vazia, forçar saídas
            ci = 0
            co = CHECKOUTS_PER_TICK + 3
        else:
            ci = CHECKINS_PER_TICK
            co = CHECKOUTS_PER_TICK

        # Checkouts
        if inside:
            candidates_out = random.sample(list(inside), min(co, len(inside)))
            for email in candidates_out:
                role = "ALUNO" if email.startswith("aluno") else "FUNCIONARIO"
                password = f"Aluno{email[5:9]}@" if role == "ALUNO" else f"Func{email[11:13]}@123"
                token = get_token(email, password, role)
                if token:
                    ok = do_checkout(email, token)
                    if ok:
                        log.debug(f"CHECKOUT: {email}")

        # Checkins (só se abaixo do limite)
        if len(inside) < MAX_SIMULTANEOUS:
            outside = [u for u in all_users if u[0] not in inside]
            candidates_in = random.sample(outside, min(ci, len(outside)))
            for email, password, role in candidates_in:
                token = get_token(email, password, role)
                if token:
                    ok = do_checkin(email, token)
                    if ok:
                        log.debug(f"CHECKIN: {email}")

        log.info(f"Tick #{tick:04d} | Dentro: {len(inside):3d} | Hora: {hour:02d}h")
        time.sleep(TICK_SECONDS)


if __name__ == "__main__":
    main()
