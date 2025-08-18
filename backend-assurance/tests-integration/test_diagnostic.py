import requests
import json

# Configuration
KEYCLOAK_CONFIG = {
    "url": "http://localhost:9090/realms/assurance/protocol/openid-connect/token",
    "client_id": "swagger-client",
    "client_secret": "RaT3qcV0fYxu5sQ2fnjM3zia9U60spts",
    "username": "mariem",
    "password": "mariem"
}

SPRING_ENDPOINTS = {
    "base_url": "http://localhost:9099",
    "health": "/actuator/health",  # Essayez ce endpoint standard Spring Boot
    "predict": "/api/fraud/predict"  # Alternative si /api/v1 n'existe pas
}

def get_keycloak_token():
    try:
        response = requests.post(
            KEYCLOAK_CONFIG["url"],
            data={
                "client_id": KEYCLOAK_CONFIG["client_id"],
                "client_secret": KEYCLOAK_CONFIG["client_secret"],
                "username": KEYCLOAK_CONFIG["username"],
                "password": KEYCLOAK_CONFIG["password"],
                "grant_type": "password"
            },
            timeout=5
        )
        response.raise_for_status()
        return response.json()["access_token"]
    except Exception as e:
        print(f"\n❌ Erreur Keycloak: {str(e)}")
        return None

def test_endpoint(url, headers=None, method="GET", json_data=None):
    try:
        if method == "GET":
            response = requests.get(url, headers=headers, timeout=5)
        else:
            response = requests.post(url, headers=headers, json=json_data, timeout=5)
        
        print(f"URL: {url}")
        print(f"Status: {response.status_code}")
        try:
            print(f"Response: {json.dumps(response.json(), indent=2)}")
        except:
            print(f"Raw Response: {response.text}")
        return response.status_code == 200
    except Exception as e:
        print(f"❌ Erreur: {str(e)}")
        return False

def main():
    print("🔍 Démarrage des tests - Mode Diagnostic")
    print("="*50)
    
    # 1. Test service ML
    print("\n[1/3] Testing ML Service...")
    ml_ok = test_endpoint("http://localhost:5000/health")
    
    # 2. Test Spring Boot sans auth
    print("\n[2/3] Testing Spring Boot (no auth)...")
    endpoints_to_test = [
        SPRING_ENDPOINTS["base_url"] + SPRING_ENDPOINTS["health"],
        SPRING_ENDPOINTS["base_url"] + "/api/v1/fraud/health",  # Votre endpoint
        SPRING_ENDPOINTS["base_url"] + "/actuator/health"       # Endpoint standard
    ]
    
    for endpoint in endpoints_to_test:
        test_endpoint(endpoint)
    
    # 3. Test avec authentification
    print("\n[3/3] Testing with authentication...")
    token = get_keycloak_token()
    if token:
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        
        # Test prediction
        test_data = {
            "contractData": {
                "rc": 54.67,
                "dRec": 20,
                "incendie": 41.5,
                "vol": 42,
                "totalPrimeNette": 183.17
            }
        }
        
        prediction_endpoints = [
            SPRING_ENDPOINTS["base_url"] + "/api/v1/fraud/predict",
            SPRING_ENDPOINTS["base_url"] + "/api/fraud/predict"
        ]
        
        for endpoint in prediction_endpoints:
            test_endpoint(endpoint, headers, "POST", test_data)

if __name__ == "__main__":
    main()
