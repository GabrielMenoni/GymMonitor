import os
import sys
import time
import requests
from faker import Faker
from datetime import datetime, timedelta
import random

GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8080/api")
MAX_RETRIES = 30
RETRY_DELAY = 5

fake = Faker("pt_BR")

ADMIN_EMAIL = "GymMonitor@gmail.com"
ADMIN_PASSWORD = "GymAdmin@123"

NUM_FUNCIONARIOS = 10
NUM_ALUNOS = 2000


def wait_for_gateway():
    print(f"Aguardando gateway em {GATEWAY_URL}...")
    for attempt in range(MAX_RETRIES):
        try:
            base = GATEWAY_URL[: GATEWAY_URL.rfind("/api")] if "/api" in GATEWAY_URL else GATEWAY_URL
            r = requests.get(f"{base}/actuator/health", timeout=5)
            if r.status_code == 200:
                print("Gateway disponível.")
                return
        except requests.exceptions.ConnectionError:
            pass
        print(f"  Tentativa {attempt + 1}/{MAX_RETRIES} — aguardando {RETRY_DELAY}s...")
        time.sleep(RETRY_DELAY)
    print("ERRO: Gateway não ficou disponível a tempo.")
    sys.exit(1)


def login_admin():
    r = requests.post(
        f"{GATEWAY_URL}/auth/admin/login",
        json={"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD},
        timeout=10,
    )
    r.raise_for_status()
    token = r.json()["token"]
    print(f"Admin autenticado: {ADMIN_EMAIL}")
    return token


def headers(token):
    return {"Authorization": f"Bearer {token}"}


def cadastrar_funcionario(token, index):
    nome = fake.name()
    email = f"funcionario{index:02d}@gymmonitor.com"
    cargos = ["Instrutor", "Recepcionista", "Personal Trainer", "Coordenador", "Gerente"]
    payload = {
        "name": nome,
        "email": email,
        "position": random.choice(cargos),
        "salary": round(random.uniform(2500.0, 8000.0), 2),
        "password": f"Func{index:02d}@123",
    }
    r = requests.post(
        f"{GATEWAY_URL}/auth/funcionarios/cadastro",
        json=payload,
        headers=headers(token),
        timeout=10,
    )
    if r.status_code in (200, 201):
        return email
    if r.status_code in (400, 409):
        print(f"  Funcionário {email} já existe, pulando.")
        return email
    r.raise_for_status()


def cadastrar_aluno(token, index):
    nome = fake.name()
    email = f"aluno{index:04d}@gymmonitor.com"
    birth = fake.date_of_birth(minimum_age=16, maximum_age=60).isoformat()
    due = (datetime.now() + timedelta(days=random.randint(1, 30))).strftime("%Y-%m-%d")
    payload = {
        "name": nome,
        "email": email,
        "birthDate": birth,
        "monthlyPaymentDueDate": due,
        "password": f"Aluno{index:04d}@",
    }
    r = requests.post(
        f"{GATEWAY_URL}/auth/alunos/cadastro",
        json=payload,
        headers=headers(token),
        timeout=10,
    )
    if r.status_code in (200, 201):
        return email
    if r.status_code == 409:
        return email  # já existe
    print(f"  AVISO: aluno {email} retornou {r.status_code}: {r.text[:100]}")
    return None


def is_already_seeded():
    try:
        r = requests.post(
            f"{GATEWAY_URL}/auth/funcionarios/login",
            json={"email": "funcionario01@gymmonitor.com", "password": "Func01@123"},
            timeout=10,
        )
        return r.status_code == 200
    except requests.exceptions.RequestException:
        return False


def main():
    wait_for_gateway()

    if is_already_seeded():
        print("Seed já foi executado anteriormente. Pulando.")
        sys.exit(0)

    token = login_admin()

    # Funcionários
    print(f"\nCadastrando {NUM_FUNCIONARIOS} funcionários...")
    funcionarios = []
    for i in range(1, NUM_FUNCIONARIOS + 1):
        email = cadastrar_funcionario(token, i)
        if email:
            funcionarios.append(email)
        if i % 5 == 0:
            print(f"  {i}/{NUM_FUNCIONARIOS} funcionários cadastrados")

    # Alunos (precisa de token de funcionário para cadastrar)
    print(f"\nCadastrando {NUM_ALUNOS} alunos...")
    if not funcionarios:
        print("ERRO: Nenhum funcionário foi cadastrado com sucesso. Abortando seed de alunos.")
        sys.exit(1)

    func_token = None
    for i, func_email in enumerate(funcionarios, start=1):
        index = int(func_email.replace("funcionario", "").replace("@gymmonitor.com", ""))
        func_password = f"Func{index:02d}@123"
        func_login_r = requests.post(
            f"{GATEWAY_URL}/auth/funcionarios/login",
            json={"email": func_email, "password": func_password},
            timeout=10,
        )
        if func_login_r.status_code == 200:
            func_token = func_login_r.json()["token"]
            print(f"  Usando {func_email} para cadastrar alunos.")
            break
        print(f"  Login falhou para {func_email} ({func_login_r.status_code}), tentando próximo...")

    if func_token is None:
        print("ERRO: Nenhum funcionário conseguiu autenticar. Abortando seed de alunos.")
        sys.exit(1)

    alunos_ok = 0
    for i in range(1, NUM_ALUNOS + 1):
        email = cadastrar_aluno(func_token, i)
        if email:
            alunos_ok += 1
        if i % 100 == 0:
            print(f"  {i}/{NUM_ALUNOS} alunos processados ({alunos_ok} cadastrados)")

    print(f"\n=== Seed concluído ===")
    print(f"  Admin:        1 (GymMonitor@gmail.com / GymAdmin@123)")
    print(f"  Funcionários: {len(funcionarios)}")
    print(f"  Alunos:       {alunos_ok}")
    print(f"\nCredenciais dos funcionários: funcionario01@gymmonitor.com / Func01@123 ... funcionario10@gymmonitor.com / Func10@123")
    print(f"Credenciais dos alunos: aluno0001@gymmonitor.com / Aluno0001@ ... aluno2000@gymmonitor.com / Aluno2000@")


if __name__ == "__main__":
    main()
