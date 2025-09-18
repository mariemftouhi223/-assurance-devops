FROM python:3.9-slim

# Répertoire de travail
WORKDIR /app

# Dépendances système (comme tu avais)
RUN apt-get update && apt-get install -y gcc g++ && rm -rf /var/lib/apt/lists/*

# Dépendances Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Code + modèles
COPY . .

# Garde-fou : échoue le build si un modèle manque
RUN test -f models/isolation_forest_sinistre_model.pkl \
 && test -f models/fraud_model.pkl \
 && test -f models/scaler_sinistre.pkl

# Logs non bufférisés
ENV PYTHONUNBUFFERED=1

EXPOSE 5000
CMD ["python", "ml_service.py"]
