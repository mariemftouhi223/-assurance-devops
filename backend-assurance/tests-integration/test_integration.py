import requests
import json

# Configuration Keycloak
KEYCLOAK_URL = "http://localhost:9090/realms/assurance/protocol/openid-connect/token"
CLIENT_ID = "swagger-client"
CLIENT_SECRET = "RaT3qcV0fYxu5sQ2fnjM3zia9U60spts" # Assurez-vous que c'est le bon secret
USERNAME = "mariem"
PASSWORD = "mariem"

def get_keycloak_token( ):
    """Récupère un token JWT depuis Keycloak"""
    payload = {
        'client_id': CLIENT_ID,
        'client_secret': CLIENT_SECRET,
        'username': USERNAME,
        'password': PASSWORD,
        'grant_type': 'password'
    }

    try:
        response = requests.post(KEYCLOAK_URL, data=payload)
        response.raise_for_status()
        return response.json()['access_token']
    except Exception as e:
        print(f"\n❌ Erreur d'authentification Keycloak:\n{str(e)}")
        print(f"Response: {response.text if 'response' in locals() else 'N/A'}")
        return None

def test_ml_service():
    """Teste le service ML Flask"""
    print("\n=== Test du service ML ===")
    try:
        response = requests.get("http://localhost:5000/health", timeout=5 )
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
        return response.status_code == 200
    except Exception as e:
        print(f"❌ Erreur: {str(e)}")
        return False

def test_springboot_service(token):
    """Teste le service Spring Boot avec authentification"""
    if not token:
        return False

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    print("\n=== Test Spring Boot ===")

    # Test santé
    try:
        health_resp = requests.get(
            "http://localhost:9099/api/v1/fraud/health",
            headers=headers,
            timeout=5
        )
        print(f"Health Status: {health_resp.status_code}")
        print(f"Health Response: {health_resp.json()}")
    except Exception as e:
        print(f"❌ Health Check Failed: {str(e)}")
        return False

    # Test prédiction
    print("\n=== Test Prédiction ===")
    # UTILISEZ LE PAYLOAD COMPLET POUR CORRESPONDRE AU MODÈLE ML
    test_data = {
        "contractData": {
            "contractId": "CONTRAT_TEST_001",
            "clientId": "client_test_001",
            "amount": 183.17,
            "startDate": "2024-01-01",
            "endDate": "2025-01-01",
            "rc": 54.67,
            "dRec": 20,
            "incendie": 41.5,
            "vol": 42,
            "dommagesAuVehicule": 0,
            "dommagesEtCollision": 0,
            "brisDeGlaces": 0,
            "pta": 25,
            "individuelleAccident": 0,
            "catastropheNaturelle": 0,
            "emeuteMouvementPopulaire": 0,
            "volRadioCassette": 0,
            "assistanceEtCarglass": 0,
            "carglass": 0,
            "totalTaxe": 21.41,
            "frais": 20,
            "totalPrimeNette": 183.17,
            "capitaleInc": 9000,
            "capitaleVol": 9000,
            "capitaleDv": 0,
            "valeurCatalogue": 9000,
            "valeurVenale": 9000
        }
    }

    try:
        pred_resp = requests.post(
            "http://localhost:9099/api/v1/fraud/predict",
            headers=headers,
            json=test_data,
            timeout=10
        )
        print(f"Prediction Status: {pred_resp.status_code}")
        print("Prediction Response:")
        print(json.dumps(pred_resp.json(), indent=2))
        return pred_resp.status_code == 200
    except Exception as e:
        print(f"❌ Prediction Failed: {str(e)}")
        return False

def main():
    print("🚀 Démarrage des tests d'intégration")
    print("="*50)

    # 1. Test service ML
    if not test_ml_service():
        print("\n❌ Le test du service ML a échoué")
        return

    # 2. Authentification Keycloak
    print("\n🔑 Authentification Keycloak...")
    token = get_keycloak_token()
    if not token:
        return

    # 3. Test Spring Boot
    if not test_springboot_service(token):
        print("\n❌ Les tests Spring Boot ont échoué")
        return

    print("\n✅ Tous les tests ont réussi !")

if __name__ == "__main__":
    main()
