# Architecture

```mermaid
flowchart LR
  UI[React Vite frontend] --> G[API Gateway :8080]
  G --> A[Auth :8090]
  G --> P[Product :8082]
  G --> I[Inventory :8083]
  G --> S[Supplier :8084]
  G --> O[Order :8085]
  A --> DB[(PostgreSQL)]
  P --> DB
  I --> DB
  S --> DB
  O --> DB
  O -. synchronous reservation .-> I
```

Services are independently deployable Spring Boot applications. PostgreSQL is shared as an infrastructure instance, while tables are owned by service schema prefixes. Flyway runs on service startup. The gateway contains only routing and cross-origin policy; domain logic stays in the owning service.
