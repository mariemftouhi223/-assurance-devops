import joblib

model_path = 'models/fraud_model_v2.pkl'
model = joblib.load(model_path)

# Adapter la fonction de prédiction selon le nouveau modèle
def predict(input_data):
    # traitement des données en entrée
    # ...
    prediction = model.predict(input_data)
    confidence = model.predict_proba(input_data)
    # retourner un dictionnaire avec le résultat attendu
    return {
        "isFraud": bool(prediction),
        "confidence": float(confidence[0][1]),
        "riskLevel": "HIGH" if confidence[0][1] > 0.8 else "LOW"
    }
