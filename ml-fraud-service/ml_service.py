from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import numpy as np
import pandas as pd
import logging
import time
from datetime import datetime
import os

# ----------------------------------
# Logging
# ----------------------------------
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ----------------------------------
# App
# ----------------------------------
app = Flask(__name__)
CORS(app)

# ----------------------------------
# Chemins des modèles
# ----------------------------------
MODEL_PATH = "models/isolation_forest_sinistre_model.pkl"
SCALER_PATH = "models/scaler_sinistre.pkl"
FEATURES_PATH = "models/features_sinistre.pkl"   # doit contenir {'features': [...]} (colonnes finales)

# ----------------------------------
# Globals
# ----------------------------------
model = None
scaler = None
features_info = None

# ----------------------------------
# Configuration
# ----------------------------------
MODEL_VERSION = "1.1.0"
SERVICE_NAME = "fraud-detection-v1-sinistre"

# ----------------------------------
# Utils
# ----------------------------------
def load_models():
    """Charger modèle, scaler et infos de features."""
    global model, scaler, features_info
    try:
        if os.path.exists(MODEL_PATH):
            model = joblib.load(MODEL_PATH)
            logger.info("Modèle IsolationForest chargé avec succès")
        else:
            logger.error(f"Fichier modèle non trouvé : {MODEL_PATH}")
            return False

        if os.path.exists(SCALER_PATH):
            scaler = joblib.load(SCALER_PATH)
            logger.info("Scaler chargé avec succès")
        else:
            logger.error(f"Fichier scaler non trouvé : {SCALER_PATH}")
            return False

        if os.path.exists(FEATURES_PATH):
            features_info = joblib.load(FEATURES_PATH)
            if not isinstance(features_info, dict) or "features" not in features_info:
                logger.error("features_sinistre.pkl invalide : clé 'features' manquante")
                return False
            logger.info("Informations des features chargées avec succès")
        else:
            logger.error(f"Fichier d'informations des features non trouvé : {FEATURES_PATH}")
            return False

        return True
    except Exception as e:
        logger.error(f"Erreur lors du chargement des modèles : {str(e)}")
        return False


def validate_input_data(data):
    """Valider les données d'entrée."""
    if not data:
        return False, "Aucune donnée fournie"
    if 'contractData' not in data:
        return False, "Le champ 'contractData' est requis"
    if 'sinistreData' not in data:
        return False, "Le champ 'sinistreData' est requis"
    return True, "Données valides"


def extract_features(contract_data, sinistre_data):
    """
    Extraire les features et les aligner EXACTEMENT sur celles du training.
    Encodage catégoriel déterministe (one-hot) puis reindex sur features_info['features'].
    """
    # Fusion (contrat + sinistre)
    df = pd.DataFrame([dict(contract_data, **sinistre_data)])

    # Dates → durée du contrat (jours)
    for col in ['EFFET_CONTRAT', 'DATE_EXPIRATION']:
        if col in df.columns:
            df[col] = pd.to_datetime(df[col], errors='coerce')

    if 'DATE_EXPIRATION' in df.columns and 'EFFET_CONTRAT' in df.columns:
        df['duree_contrat'] = (df['DATE_EXPIRATION'] - df['EFFET_CONTRAT']).dt.days

    # Montants de règlement
    reglement_cols = [c for c in df.columns if c.startswith("REGLEMENT_")]
    for c in reglement_cols:
        df[c] = pd.to_numeric(df[c], errors='coerce')
    if reglement_cols:
        df['montant_total_regle'] = df[reglement_cols].sum(axis=1)
        df['nb_types_reglement'] = (df[reglement_cols] > 0).sum(axis=1)
    else:
        df['montant_total_regle'] = 0
        df['nb_types_reglement'] = 0

    # Catégorielles (adapter si besoin : même liste qu'au training)
    cat_cols = ['usage', 'CODE_INTERMEDIAIRE', 'NATURE_SINISTRE', 'LIEU_ACCIDENT']
    for c in cat_cols:
        if c in df.columns:
            df[c] = df[c].astype(str)

    # One-hot déterministe
    dummies = pd.get_dummies(
        df[[c for c in cat_cols if c in df.columns]],
        prefix=[c for c in cat_cols if c in df.columns],
        dtype=int
    ) if any(c in df.columns for c in cat_cols) else pd.DataFrame(index=df.index)

    # Numériques = tout sauf cat_cols existantes
    num_df = df.drop(columns=[c for c in cat_cols if c in df.columns], errors='ignore')

    X = pd.concat([num_df, dummies], axis=1)

    # Aligner sur l'ordre exacte des features du training
    final_features = features_info['features']
    X = X.reindex(columns=final_features, fill_value=0)

    # Manquants
    X = X.fillna(0)

    return X


# ----------------------------------
# Routes
# ----------------------------------
@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "healthy",
        "model_loaded": model is not None,
        "scaler_loaded": scaler is not None,
        "features_loaded": features_info is not None,
        "version": MODEL_VERSION,
        "service": SERVICE_NAME,
        "timestamp": datetime.now().isoformat()
    })


@app.route('/predict', methods=['POST'])
def predict():
    """Prédiction de fraude sur sinistres (IsolationForest)."""
    start = time.time()
    try:
        data = request.get_json()
        ok, msg = validate_input_data(data)
        if not ok:
            return jsonify({"error": msg, "code": "VALIDATION_ERROR"}), 400

        if model is None or scaler is None or features_info is None:
            return jsonify({"error": "Modèles non disponibles", "code": "MODEL_UNAVAILABLE"}), 503

        contract_data = data['contractData']
        sinistre_data = data['sinistreData']

        # Features alignées + scaling
        X = extract_features(contract_data, sinistre_data)
        Xs = scaler.transform(X)

        # IsolationForest : -1 anomalie (fraude), 1 normal
        y_pred = model.predict(Xs)[0]
        is_fraud = bool(y_pred == -1)

        # Score d'anomalie stable : plus GRAND = plus anormal
        # score_samples renvoie ~(-densité); on inverse le signe pour avoir positif (intuition)
        raw_anom = float(-model.score_samples(Xs)[0])

        # Normalisation 0..1 (bornes fixes ou issues du training si présentes)
        # Si tu as sauvegardé des bornes, mets-les dans features_sinistre.pkl: {'features': [...], 'anom_min': ..., 'anom_max': ...}
        raw_min = float(features_info.get('anom_min', 0.0))
        raw_max = float(features_info.get('anom_max', 1.5))
        score = (raw_anom - raw_min) / (raw_max - raw_min + 1e-9)
        score = float(np.clip(score, 0.0, 1.0))
        score_pct = int(round(score * 100))

        # Niveaux
        if score >= 0.80:
            level = "CRITICAL"
        elif score >= 0.60:
            level = "HIGH"
        elif score >= 0.40:
            level = "MEDIUM"
        else:
            level = "LOW"

        # Pour compatibilité avec d'anciens écrans : "confidence" ~ score
        confidence = score

        resp = {
            "prediction": {
                "isFraud": is_fraud,
                "score": round(score, 4),
                "scorePct": score_pct,
                "level": level,
                "anomalyScore": round(raw_anom, 4),
                "confidence": round(confidence, 4)
            },
            "model": {
                "version": MODEL_VERSION,
                "algorithm": "IsolationForest",
                "type": "real"
            },
            "metadata": {
                "requestId": data.get('metadata', {}).get('requestId', f'req-v1-{int(time.time())}'),
                "processingTimeMs": int((time.time() - start) * 1000),
                "timestamp": datetime.now().isoformat(),
                "service": SERVICE_NAME
            }
        }
        return jsonify(resp)

    except Exception as e:
        logger.error(f"Erreur lors de la prédiction: {str(e)}")
        return jsonify({"error": f"Erreur interne: {str(e)}", "code": "INTERNAL_ERROR"}), 500


# ----------------------------------
# Main
# ----------------------------------
if __name__ == '__main__':
    logger.info(f"Démarrage du service {SERVICE_NAME} v{MODEL_VERSION}")
    if not load_models():
        logger.error("Impossible de charger les modèles. Arrêt du service.")
        exit(1)
    logger.info("Service prêt à recevoir des requêtes")
    app.run(host='0.0.0.0', port=5000, debug=False)
