# 🏥 Patient Service Platform (Microservice Architecture)

This is a project built to explore microservices architecture using Spring Boot, Kafka, gRPC, and Docker. It simulates a simple healthcare backend with authentication, billing, analytics, and an API gateway.

---

## 🧩 Services Overview

| Service           | Description   | Port |
|-------------------|---------------| ---- |
| **patient-service**   | Main service for storing and managing patient data (uses `data.sql`) | 4000
| **auth-service**      | Handles authentication and authorization using Spring Security and JWT | 4005
| **analytics-service** | Listens to Kafka events (via a consumer), purpose: event-based processing | 4002
| **billing-service**   | Exposes a gRPC service to manage billing and communicate with other services | 4001, 9001 on gRPC
| **api-gateway**       | Entry point for all requests, includes JWT validation via JWT validation filter that calls /validate in auth-service before routing requests. | 4004

---

## 🔧 Stack

- Java 17
- Spring Boot 3.5.0
- Spring Security (JWT)
- Apache Kafka
- gRPC (Java implementation)
- Docker / Docker Compose
- LocalStack (for AWS mock services)

---

## 🚀 Getting Started

### Prerequisites

- Docker 
- Java 17


### Clone and Run

```bash
git clone https://github.com/ramordeeple/patient-management.git
cd patient-service-platform
docker compose up --build
