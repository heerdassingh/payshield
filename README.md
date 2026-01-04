# PayShield - Transaction Processing System

A production-grade, real-time transaction processing system with AI-powered fraud detection, built with Java Spring Boot, Kafka, MySQL, MongoDB, and Redis.

## Architecture

```
┌──────────────┐     ┌─────────────┐     ┌──────────────────┐     ┌──────────────┐
│   REST API   │────▶│   Kafka     │────▶│  User Validator  │────▶│   Kafka      │
│  (port 8080) │     │ (incoming)  │     │   (MySQL)        │     │ (validated)  │
└──────────────┘     └─────────────┘     └──────────────────┘     └──────────────┘
                                                                         │
                     ┌─────────────┐     ┌──────────────────┐           │
                     │   Alerts    │◀────│  AI Fraud Det.   │◀──────────┘
                     │  (port 8082)│     │  (Java ML)       │
                     └─────────────┘     └──────────────────┘
                                                │
                     ┌─────────────┐     ┌──────▼──────┐
                     │  MongoDB    │◀────│  Processor  │
                     │ (records)   │     │  (port 8081)│
                     └─────────────┘     └─────────────┘
```

## Tech Stack

- **Backend**: Spring Boot 3.2 (Java 17)
- **Messaging**: Apache Kafka
- **AI/ML**: Java ML (Smile library)
- **Databases**: MySQL (users), MongoDB (transactions)
- **Caching**: Redis
- **Monitoring**: Prometheus + Grafana

## Modules

| Module | Port | Description |
|--------|------|-------------|
| payshield-api | 8080 | REST API Gateway - publishes to Kafka |
| payshield-processor | 8081 | Transaction processor with fraud detection |
| payshield-alerts | 8082 | Alert consumer and notifications |
| payshield-common | - | Shared models, DTOs, repositories |

## Quick Start

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Build the Project
```bash
mvn clean install -DskipTests
```

### 3. Start Services (in separate terminals)

**Terminal 1 - API:**
```bash
mvn spring-boot:run -pl payshield-api
```

**Terminal 2 - Processor:**
```bash
mvn spring-boot:run -pl payshield-processor
```

**Terminal 3 - Alerts:**
```bash
mvn spring-boot:run -pl payshield-alerts
```

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

### Get User Transactions
```bash
curl http://localhost:8080/api/v1/transactions/user/{userId}
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
| Swagger UI | http://localhost:8080/swagger-ui.html |
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

The AI fraud detection analyzes:
- Transaction amount (normalized)
- Transaction frequency (hourly/daily)
- Time of day anomaly
- Amount deviation from normal
- High velocity patterns

## Retry Mechanism

Failed transactions are automatically retried with exponential backoff:
- Retry interval: 2^retryCount * 60 seconds
- Maximum retries: 5
- Status tracked in `failed_transactions` collection
