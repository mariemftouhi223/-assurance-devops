from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import numpy as np
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
MODEL_PATH = "models/random_forest_fraude_model.pkl"
SCALER_PATH = "models/scaler_fraude.pkl"

# ----------------------------------
# Globals
# ----------------------------------
model = None
scaler = None

# ----------------------------------
# Features attendues (ordre EXACT du training)
# ----------------------------------
FEATURE_COLUMNS = [
    'RC', 'D_rec', 'incendie', 'vol', 'DOMMAGES_AU_VEHICULE',
    'DOMMAGES_ET_COLLISION', 'BRIS_DE_GLACES', 'PTA', 'INDIVIDUELLE_ACCIDENT',
    'CATASTROPHE_NATURELLE', 'EMEUTE_MOUVEMENT_POPULAIRE', 'VOL_RADIO_CASSETTE',
    'Assistanceet_carglass', 'carglass', 'TOTAL_TAXE', 'FRAIS',
    'TOTAL_PRIME_NETTE', 'capitale_inc', 'capitale_vol', 'capitale_DV',
    'VALEUR_CATALOGUE', 'VALEUR_VENALE'
]

# ----------------------------------
# Utils
# ----------------------------------
def load_models():
    """Charger le modèle et le scaler."""
    global model, scaler
    try:
        if os.path.exists(MODEL_PATH):
            model = joblib.load(MODEL_PATH)
            logger.info("Modèle RandomForest chargé avec succès")
        else:
            logger.error(f"Fichier modèle non trouvé : {MODEL_PATH}")
            return False

        if os.path.exists(SCALER_PATH):
            scaler = joblib.load(SCALER_PATH)
            logger.info("Scaler chargé avec succès")
        else:
            logger.warning(f"Fichier scaler non trouvé : {SCALER_PATH} (continuer sans)")
            scaler = None

        return True
    except Exception as e:
        logger.error(f"Erreur lors du chargement des modèles : {str(e)}")
        return False


def validate_input_data(data):
    """Valider les données d'entrée."""
    if not isinstance(data, dict):
        return False, "Les données doivent être un objet JSON"

    if 'contractData' not in data:
        return False, "Le champ 'contractData' est requis"

    contract = data['contractData']

    missing = [c for c in FEATURE_COLUMNS if c not in contract]
    if missing:
        return False, f"Colonnes manquantes : {missing}"

    # Numériques ?
    for c in FEATURE_COLUMNS:
        try:
            float(contract[c])
        except (ValueError, TypeError):
            return False, f"La valeur de '{c}' doit être numérique"

    return True, "Données valides"


# ----------------------------------
# Routes
# ----------------------------------
@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        "status": "healthy",
        "model_loaded": model is not None,
        "scaler_loaded": scaler is not None,
        "timestamp": datetime.now().isoformat(),
        "version": "2.0.0",
        "service": "fraud-detection-v2"
    })


@app.route('/predict', methods=['POST'])
def predict():
    """Endpoint principal de prédiction."""
    start_time = time.time()
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "Aucune donnée JSON fournie", "code": "INVALID_INPUT"}), 400

        ok, msg = validate_input_data(data)
        if not ok:
            return jsonify({"error": msg, "code": "VALIDATION_ERROR"}), 400

        if model is None:
            return jsonify({"error": "Modèle ML non disponible", "code": "MODEL_UNAVAILABLE"}), 503

        contract = data['contractData']
        features = [float(contract[c]) for c in FEATURE_COLUMNS]
        X = np.array([features])

        if scaler is not None:
            X = scaler.transform(X)

        y_pred = model.predict(X)[0]
        proba = model.predict_proba(X)[0]  # [p_non_fraude, p_fraude]
        fraud_proba = float(proba[1])
        confidence = float(max(proba))  # confiance globale

        # On harmonise avec le service V1
        score = fraud_proba
        score_pct = int(round(score * 100))

        if score >= 0.80:
            level = "CRITICAL"
        elif score >= 0.60:
            level = "HIGH"
        elif score >= 0.40:
            level = "MEDIUM"
        else:
            level = "LOW"

        resp = {
            "prediction": {
                "isFraud": bool(y_pred),
                "confidence": round(confidence, 4),
                "fraudProbability": round(fraud_proba, 4),
                "score": round(score, 4),
                "scorePct": score_pct,
                "level": level
            },
            "model": {
                "version": "2.0.0",
                "featuresUsed": len(FEATURE_COLUMNS),
                "algorithm": "RandomForest",
                "scalerUsed": scaler is not None
            },
            "metadata": {
                "requestId": data.get('metadata', {}).get('requestId', 'unknown'),
                "processingTime": int((time.time() - start_time) * 1000),
                "timestamp": datetime.now().isoformat(),
                "service": "fraud-detection-v2"
            }
        }

        logger.info(f"Prédiction V2 - Fraude: {bool(y_pred)}, ScorePct: {score_pct}% ({level})")
        return jsonify(resp)

    except Exception as e:
        logger.error(f"Erreur lors de la prédiction : {str(e)}")
        return jsonify({
            "error": "Erreur interne du serveur",
            "code": "INTERNAL_ERROR",
            "details": str(e)
        }), 500


@app.route('/model/info', methods=['GET'])
def model_info():
    if model is None:
        return jsonify({"error": "Modèle non chargé"}), 503

    return jsonify({
        "model": {
            "type": type(model).__name__,
            "features": FEATURE_COLUMNS,
            "featureCount": len(FEATURE_COLUMNS),
            "version": "2.0.0",
            "scalerAvailable": scaler is not None
        },
        "status": "loaded",
        "timestamp": datetime.now().isoformat()
    })


# ----------------------------------
# Main
# ----------------------------------
if __name__ == '__main__':
    if not load_models():
        logger.error("Impossible de charger les modèles. Arrêt du service.")
        exit(1)
    app.run(host='0.0.0.0', port=5001, debug=False)
