import requests
import json

KEYCLOAK_URL = "http://localhost:9090/realms/assurance/protocol/openid-connect/token"
CLIENT_ID = "swagger-client"
CLIENT_SECRET = "RaT3qcV0fYxu5sQ2fnjM3zia9U60spts"
USERNAME = "mariem"
PASSWORD = "mariem"

BASE_URL = "http://localhost:9099"
ENDPOINTS = {
    "ml_health": "http://localhost:5000/health",
    "spring_health": f"{BASE_URL}/api/v1/fraud/health",
    "predict": f"{BASE_URL}/api/v1/fraud/predict"
}

def get_token():
    try:
        response = requests.post(
            KEYCLOAK_URL,
            data={
                "client_id": CLIENT_ID,
                "client_secret": CLIENT_SECRET,
                "username": USERNAME,
                "password": PASSWORD,
                "grant_type": "password"
            },
            timeout=5
        )
        response.raise_for_status()
        return response.json().get("access_token")
    except Exception as e:
        print(f"❌ Erreur Keycloak: {str(e)}")
        return None

def test_endpoint(url, method="GET", headers=None, data=None):
    try:
        if method == "GET":
            response = requests.get(url, headers=headers, timeout=5)
        else:
            response = requests.post(url, headers=headers, json=data, timeout=5)

        print(f"\n📡 Endpoint: {url}")
        print(f"🔄 Status: {response.status_code}")

        try:
            print("🟢 Response:", json.dumps(response.json(), indent=2))
        except:
            print("🟡 Response:", response.text)

        return response.status_code == 200
    except Exception as e:
        print(f"❌ Erreur: {str(e)}")
        return False

def main():
    print("🧪 Test d'intégration complet")
    print("="*50)

    print("\n[1] ✅ Test Service ML")
    test_endpoint(ENDPOINTS["ml_health"])

    print("\n[2] ✅ Test Spring Boot (no auth)")
    test_endpoint(ENDPOINTS["spring_health"])

    print("\n[3] 🔐 Test avec authentification")
    token = get_token()
    if not token:
        return

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

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

    test_endpoint(ENDPOINTS["predict"], "POST", headers, test_data)

if __name__ == "__main__":
    main()
