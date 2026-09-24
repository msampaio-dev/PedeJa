# PedeJá

Uma versão simplificada do iFood. O cliente escolhe o restaurante, monta o carrinho e paga; o restaurante recebe o pedido e atualiza o status até a entrega. O pagamento é confirmado depois, por webhook, como acontece com um gateway de verdade.

> Em construção. Este README ganha a versão completa quando o fluxo de pedido estiver pronto.

## Rodando localmente

Pré-requisitos: Java 21, Node 24 e Docker.

```bash
docker compose up -d
```

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm install && npm run dev
```

A API sobe em `http://localhost:8080` (Swagger em `/swagger-ui.html`) e o frontend em `http://localhost:5173`.

Contas de demonstração, com senha `Demo123!`:

- `cliente@demo.pedeja.local`
- `restaurante@demo.pedeja.local`

## Testes

```bash
cd backend && ./mvnw verify
```

```bash
cd frontend && npm test
```

Os testes do backend sobem um PostgreSQL descartável com Testcontainers, então o Docker precisa estar rodando.

## Estrutura

```text
backend/    API em Java 21 e Spring Boot 3.5
frontend/   React 19, Vite e TypeScript
compose.yaml  PostgreSQL e RabbitMQ para desenvolvimento
```
