FROM python:3.9-slim

WORKDIR /app

RUN apt-get update && apt-get install -y gcc g++ && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Garde-fou : échoue le build si un modèle manque
RUN test -f models/random_forest_fraude_model.pkl \
 && test -f models/fraud_model_v2.pkl \
 && test -f models/scaler_fraude.pkl

ENV PYTHONUNBUFFERED=1

EXPOSE 5001
CMD ["python", "ml_service_v2.py"]
