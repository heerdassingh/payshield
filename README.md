# PayShield — AI-Powered Payment Fraud Detection

A production-grade, real-time payment fraud detection system with **Python AI (TensorFlow Autoencoder + RAG)**, built with Java Spring Boot, Kafka, MySQL, MongoDB, Redis, and a **React.js** dashboard.

## Architecture

```
┌──────────────────┐     ┌─────────────┐     ┌──────────────────┐     ┌──────────────┐
│  React Dashboard │     │   REST API  │────▶│   Kafka          │────▶│   Processor  │
│  (port 3001)     │────▶│  (port 8080)│     │ (incoming topic) │     │  (port 8081) │
└──────────────────┘     └─────────────┘     └──────────────────┘     └──────────────┘
                                                                             │
                          ┌─────────────┐     ┌──────────────────┐          │
                          │   Alerts    │◀────│  Python AI       │◀─────────┘
                          │  (port 8082)│     │  (port 8000)     │
                          └─────────────┘     │  TF Autoencoder  │
                                              │  + RAG Pipeline  │
                                              └──────────────────┘
                                                     │
                          ┌─────────────┐     ┌──────▼──────┐
                          │  MongoDB    │◀────│  Processor  │
                          │ (records)   │     │  (port 8081)│
                          └─────────────┘     └─────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.2 |
| **Messaging** | Apache Kafka |
| **AI/ML** | Python FastAPI, TensorFlow (Deep Autoencoder) |
| **RAG Pipeline** | LangChain, ChromaDB, HuggingFace Embeddings |
| **Databases** | MySQL (users/merchants), MongoDB (transactions/alerts) |
| **Caching** | Redis |
| **Frontend** | React.js, Recharts, Vite |
| **Monitoring** | Docker, Prometheus, Grafana |

## Modules

| Module | Port | Description |
|--------|------|-------------|
| payshield-api | 8080 | REST API Gateway — publishes to Kafka, serves dashboard APIs |
| payshield-processor | 8081 | Transaction processor — calls Python AI for fraud detection |
| payshield-alerts | 8082 | Alert consumer and notifications |
| payshield-ai | 8000 | Python AI — TensorFlow Autoencoder model + RAG pipeline |
| payshield-dashboard | 3001 | React.js frontend dashboard |
| payshield-common | — | Shared models, DTOs, repositories |

## Quick Start

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Build the Java Project
```bash
mvn clean install -DskipTests
```

### 3. Start Python AI Service (standalone mode)
```bash
cd payshield-ai
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### 4. Start Spring Boot Services (in separate terminals)

**Terminal 1 — API:**
```bash
mvn spring-boot:run -pl payshield-api
```

**Terminal 2 — Processor:**
```bash
mvn spring-boot:run -pl payshield-processor
```

**Terminal 3 — Alerts:**
```bash
mvn spring-boot:run -pl payshield-alerts
```

### 5. Start React Dashboard
```bash
cd payshield-dashboard
npm install
npm run dev
```

Open **http://localhost:3001** to view the dashboard.

## API Endpoints

### Submit Transaction
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-001",
    "merchantId": "merch-001",
    "amount": 150.00,
    "currency": "USD"
  }'
```

### Check Transaction Status
```bash
curl http://localhost:8080/api/v1/transactions/{transactionId}/status
```

### Dashboard APIs
```bash
# Get stats
curl http://localhost:8080/api/v1/dashboard/stats

# Recent transactions
curl http://localhost:8080/api/v1/dashboard/recent-transactions?limit=20

# Fraud trends
curl http://localhost:8080/api/v1/dashboard/fraud-trends

# Active alerts
curl http://localhost:8080/api/v1/dashboard/alerts

# AI model status
curl http://localhost:8080/api/v1/dashboard/ai-status
```

### Python AI APIs
```bash
# Health check
curl http://localhost:8000/health

# Model status
curl http://localhost:8000/api/v1/model/status

# Direct fraud prediction
curl -X POST http://localhost:8000/api/v1/predict \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "test-001",
    "user_id": "user-001",
    "merchant_id": "merch-001",
    "amount": 5000,
    "amount_normalized": 0.5,
    "frequency_1h": 0.3,
    "frequency_24h": 0.2,
    "time_anomaly": 0.0,
    "high_amount_flag": 0.3,
    "velocity": 0.0
  }'

# RAG query
curl -X POST http://localhost:8000/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What are velocity-based fraud patterns?"}'
```

## Kafka Topics

| Topic | Purpose |
|-------|---------|
| incoming-transactions | Raw API requests |
| validated-transactions | User-validated transactions |
| processed-transactions | Completed transactions |
| fraud-alerts | Fraud detection alerts |
| user-not-found | Invalid user events |

## Monitoring

| Service | URL |
|---------|-----|
| React Dashboard | http://localhost:3001 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Python AI Docs | http://localhost:8000/docs |
| Kafka UI | http://localhost:8090 |
| Grafana | http://localhost:3000 (admin/payshield) |
| Prometheus | http://localhost:9090 |

## Test Users

| User ID | Name | Status |
|---------|------|--------|
| user-001 | John Doe | ACTIVE |
| user-002 | Jane Smith | ACTIVE |
| user-003 | Blocked User | BLOCKED |
| user-005 | Inactive User | INACTIVE |

## Test Merchants

| Merchant ID | Name | Risk Level |
|-------------|------|------------|
| merch-001 | Amazon | LOW |
| merch-002 | Netflix | LOW |
| merch-003 | Risky Casino | HIGH |

## Fraud Detection Features

The Python AI service analyzes these features using a **TensorFlow Deep Autoencoder**:

| Feature | Description | Weight |
|---------|-------------|--------|
| amount_normalized | Transaction amount relative to max (0-1) | High |
| frequency_1h | Transactions in last hour vs threshold | High |
| frequency_24h | Transactions in last 24h vs threshold | Medium |
| time_anomaly | Late-night transaction indicator (1-5 AM) | Medium |
| high_amount_flag | Amount exceeding $1K/$5K thresholds | Medium |
| velocity | Rapid transaction rate indicator | Medium |

### RAG Pipeline
The RAG (Retrieval-Augmented Generation) pipeline provides **explainable AI** by:
1. Loading fraud pattern documents into ChromaDB vector store
2. For each prediction, retrieving relevant fraud knowledge
3. Generating contextual explanations alongside the ML score

## Retry Mechanism

Failed transactions are automatically retried with exponential backoff:
- Retry interval: 2^retryCount × 60 seconds
- Maximum retries: 5
- Status tracked in `failed_transactions` collection
