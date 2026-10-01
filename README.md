# Retail Inventory Management

Pragmatic multi-module Spring Boot 3 / Java 25 inventory platform with a React TypeScript dashboard.

## Run locally

1. Copy `.env.example` to `.env` and start PostgreSQL: `docker compose up -d postgres`.
2. Build all services: `mvn clean package`.
3. Run any service with `mvn spring-boot:run -pl services/<service-name>`.
4. Start the frontend: `cd frontend && npm install && npm run dev`.

Gateway: `http://localhost:8090`; frontend: `http://localhost:5173`.

## Service map

- `api-gateway`: simple Spring Cloud Gateway routes, CORS, and request correlation.
- `auth-service`: register/login, BCrypt password hashing, signed JWT issuance.
- `product-service`: categories and products with validation and Flyway migrations.
- `inventory-service`: warehouses, stock-in, adjustment, transfer, and reservation endpoints.
- `supplier-service`: supplier CRUD.
- `order-service`: purchase and customer order lifecycle endpoints.

Each persistence service owns a schema namespace and has a service-local Flyway migration. No Eureka or Kafka is required; the gateway routes synchronously and order reservation calls inventory synchronously.

## Demo flow

Register/login, create a category and product, create a warehouse, stock the product, create a supplier and purchase order, then create/cancel/fulfill a customer order. The dashboard aggregates products, warehouses, and orders through gateway endpoints.
