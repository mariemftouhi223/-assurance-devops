FROM python:3.9-slim

WORKDIR /app

# scikit-learn a besoin de libgomp1 à l'exécution
RUN apt-get update \
 && apt-get install -y --no-install-recommends libgomp1 \
 && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Code + modèles
COPY . .

# Garde-fou : vérifie que les modèles sont bien dans l'image
RUN test -f models/isolation_forest_sinistre_model.pkl \
 && test -f models/fraud_model.pkl \
 && test -f models/scaler_sinistre.pkl

ENV PYTHONUNBUFFERED=1
EXPOSE 5000
CMD ["python", "ml_service.py"]
