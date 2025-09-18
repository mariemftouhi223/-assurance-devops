# Base + workdir
FROM python:3.9-slim

WORKDIR /app

# System deps (add libgomp1 for scikit-learn runtime)
RUN apt-get update \
 && apt-get install -y gcc g++ libgomp1 \
 && rm -rf /var/lib/apt/lists/*

# Python deps
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# App code + models
COPY . .

# Guard: ensure models are baked in
# (keep the three tests as you already did)
RUN test -f models/<MODEL_1>.pkl \
 && test -f models/<MODEL_2>.pkl \
 && test -f models/<SCALER>.pkl

ENV PYTHONUNBUFFERED=1
EXPOSE 5000
CMD ["python", "ml_service.py"]
