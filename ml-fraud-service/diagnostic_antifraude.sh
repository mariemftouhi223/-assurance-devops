#!/bin/bash

echo "=== DIAGNOSTIC APPLICATION ANTI-FRAUDE ==="
date
echo "=========================================="

echo
echo "1. VÉRIFICATION DES SERVICES"
echo "=============================="

check_port() {
    local port=$1
    local name=$2
    if netstat -an | grep ":$port " | grep LISTEN > /dev/null; then
        echo "✅ Port $port ouvert - $name démarré"
    else
        echo "❌ Port $port fermé - $name non démarré"
    fi
}

check_port 9099 "Spring Boot Backend"
check_port 4200 "Angular Frontend"
check_port 8080 "Keycloak"
check_port 5000 "Service ML Python"

echo
echo "2. TEST DES ENDPOINTS SPRING BOOT"
echo "=================================="

test_endpoint() {
    local url=$1
    local name=$2
    code=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    echo "🔍 $name: $url => HTTP $code"
}

test_endpoint http://localhost:9099/api/v1/fraud/health "Health Check"
test_endpoint http://localhost:9099/api/v1/contracts "Contrats"
test_endpoint http://localhost:9099/api/v1/fraud/predict "Prédiction"
test_endpoint http://localhost:5000/health "ML Health"
test_endpoint http://localhost:5000/predict "ML Predict"

