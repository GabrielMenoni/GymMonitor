FROM python:3.12-alpine

WORKDIR /app

ENV PYTHONUNBUFFERED=1

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY seed.py .

CMD ["python", "seed.py"]
