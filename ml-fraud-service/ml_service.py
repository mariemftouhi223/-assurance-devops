from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import numpy as np
import pandas as pd
import logging
import time
from datetime import datetime
import os

# Configuration du logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# Chemins des modèles
MODEL_PATH = "models/isolation_forest_sinistre_model.pkl"
SCALER_PATH = "models/scaler_sinistre.pkl"
FEATURES_PATH = "models/features_sinistre.pkl"

# Variables globales pour les modèles
model = None
scaler = None
features_info = None

# Configuration
MODEL_VERSION = "1.1.0"
SERVICE_NAME = "fraud-detection-v1-sinistre"

def load_models():
    """Charger le modèle, le scaler et les features"""
    global model, scaler, features_info

    try:
        if os.path.exists(MODEL_PATH):
            model = joblib.load(MODEL_PATH)
            logger.info("Modèle Isolation Forest réel chargé avec succès")
        else:
            logger.error(f"Fichier modèle non trouvé : {MODEL_PATH}")
            return False

        if os.path.exists(SCALER_PATH):
            scaler = joblib.load(SCALER_PATH)
            logger.info("Scaler réel chargé avec succès")
        else:
            logger.error(f"Fichier scaler non trouvé : {SCALER_PATH}")
            return False

        if os.path.exists(FEATURES_PATH):
            features_info = joblib.load(FEATURES_PATH)
            logger.info("Informations des features chargées avec succès")
        else:
            logger.error(f"Fichier d'informations des features non trouvé : {FEATURES_PATH}")
            return False

        return True

    except Exception as e:
        logger.error(f"Erreur lors du chargement des modèles : {str(e)}")
        return False

def validate_input_data(data):
    """Valider les données d'entrée"""
    if not data:
        return False, "Aucune donnée fournie"

    if 'contractData' not in data:
        return False, "Le champ 'contractData' est requis"

    if 'sinistreData' not in data:
        return False, "Le champ 'sinistreData' est requis"

    return True, "Données valides"

def extract_features(contract_data, sinistre_data):
    """Extraire et préparer les features pour le modèle"""
    # Créer un DataFrame à partir des données d'entrée
    input_df = pd.DataFrame([dict(contract_data, **sinistre_data)])

    # Prétraitement des dates
    for col in ['EFFET_CONTRAT', 'DATE_EXPIRATION']:
        if col in input_df.columns:
            input_df[col] = pd.to_datetime(input_df[col], errors='coerce')

    # Feature Engineering
    if 'DATE_EXPIRATION' in input_df.columns and 'EFFET_CONTRAT' in input_df.columns:
        input_df['duree_contrat'] = (input_df['DATE_EXPIRATION'] - input_df['EFFET_CONTRAT']).dt.days

    reglement_cols = [col for col in input_df.columns if col.startswith("REGLEMENT_")]
    for col in reglement_cols:
        input_df[col] = pd.to_numeric(input_df[col], errors='coerce')
    input_df['montant_total_regle'] = input_df[reglement_cols].sum(axis=1)
    input_df['nb_types_reglement'] = (input_df[reglement_cols] > 0).sum(axis=1)

    cat_cols = ['usage', 'CODE_INTERMEDIAIRE', 'NATURE_SINISTRE', 'LIEU_ACCIDENT']
    for col in cat_cols:
        if col in input_df.columns:
            input_df[col] = input_df[col].astype(str)
            # Utiliser un encodage simple pour les nouvelles catégories
            input_df[col] = pd.factorize(input_df[col])[0]

    # Sélectionner et ordonner les features
    final_features = features_info['features']
    input_df = input_df.reindex(columns=final_features, fill_value=0)

    # Remplir les NaN avec la médiane (ou 0 si non disponible)
    input_df = input_df.fillna(0)

    return input_df

@app.route('/predict', methods=['POST'])
def predict():
    """Endpoint de prédiction de fraude sur les sinistres"""
    start_time = time.time()

    try:
        data = request.get_json()
        valid, msg = validate_input_data(data)
        if not valid:
            return jsonify({"error": msg, "code": "VALIDATION_ERROR"}), 400

        if model is None or scaler is None or features_info is None:
            return jsonify({"error": "Modèles non disponibles", "code": "MODEL_UNAVAILABLE"}), 503

        # Extraire les données
        contract_data = data['contractData']
        sinistre_data = data['sinistreData']

        # Préparer les données pour la prédiction
        features_df = extract_features(contract_data, sinistre_data)
        scaled_features = scaler.transform(features_df)

        # Faire la prédiction
        prediction = model.predict(scaled_features)[0]
        # Isolation Forest retourne -1 pour les anomalies (fraude) et 1 pour les normaux
        is_fraud = bool(prediction == -1)

        # Calculer un score de confiance (plus le score est bas, plus c'est une anomalie)
        anomaly_score = model.decision_function(scaled_features)[0]
        confidence = 1 - (anomaly_score - model.offset_) / 2

        processing_time = int((time.time() - start_time) * 1000)

        response = {
            "prediction": {
                "isFraud": is_fraud,
                "confidence": round(confidence, 4),
                "anomalyScore": round(anomaly_score, 4)
            },
            "model": {
                "version": MODEL_VERSION,
                "algorithm": "IsolationForest",
                "type": "real"
            },
            "metadata": {
                "requestId": data.get('metadata', {}).get('requestId', f'req-v1-{int(time.time())}'),
                "processingTimeMs": processing_time,
                "timestamp": datetime.now().isoformat(),
                "service": SERVICE_NAME
            }
        }

        return jsonify(response)

    except Exception as e:
        logger.error(f"Erreur lors de la prédiction: {str(e)}")
        return jsonify({"error": f"Erreur interne: {str(e)}", "code": "INTERNAL_ERROR"}), 500

if __name__ == '__main__':
    logger.info(f"Démarrage du service {SERVICE_NAME} v{MODEL_VERSION}")
    if not load_models():
        logger.error("Impossible de charger les modèles. Arrêt du service.")
        exit(1)

    logger.info("Service prêt à recevoir des requêtes")
    app.run(host='0.0.0.0', port=5000, debug=False)
