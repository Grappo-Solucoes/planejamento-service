# ENVIRONMENT — planejamento-service

Variáveis de ambiente, perfis e operação local do `planejamento-service`.
O serviço é o único dono do schema `agendamento` (Postgres) e conversa com o
`busco-api` pelo RabbitMQ conforme `docs/events/v1/CONTRACT.md`.

## Subir em ambiente limpo (dev)

```bash
# 1. Sobe Postgres, Redis e RabbitMQ de desenvolvimento
docker compose -f docker-compose.dev.yml up -d

# 2. Carrega o exemplo de variáveis de ambiente
cp .env.example .env
set -a && . ./.env && set +a

# 3. Sobe o serviço (perfil integration; Flyway cria o schema agendamento)
bash ./mvnw spring-boot:run
```

Probes: `GET http://localhost:8080/actuator/health/liveness` e
`GET http://localhost:8080/actuator/health/readiness` (públicas — respondem `200` sem token;
o `SecurityConfig` libera `/actuator/health/**`). O restante do Actuator e os endpoints de
negócio continuam exigindo JWT.

Métricas: `/actuator/prometheus` permanece **autenticado** (decisão registrada em `k8s/README.md`);
o scrape exige token (`Authorization: Bearer …`) ou network policy, e o endpoint só responde com
`micrometer-registry-prometheus` no classpath.

## Variáveis

| Variável | Obrigatória | Default | Descrição |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | não | — | Use `integration` no cluster (desliga o auto-processamento local). |
| `SPRING_DATASOURCE_URL` | sim | — | JDBC do Postgres; em dev `jdbc:postgresql://localhost:5432/planejamento?sslmode=disable`. |
| `SPRING_DATASOURCE_USERNAME` | sim | — | Usuário dono do schema `agendamento`. |
| `SPRING_DATASOURCE_PASSWORD` | sim | — | Senha do banco. |
| `SPRING_DATASOURCE_HIKARI_DATA_SOURCE_PROPERTIES_SSLMODE` | não | `require` | Use `disable` apenas em dev local. |
| `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT` | sim | — | `org.hibernate.dialect.PostgreSQLDialect`. |
| `REDIS_HOST` | sim | — | Host do Redis. |
| `REDIS_PORT` | sim | — | Porta do Redis. |
| `RABBITMQ_HOST` | sim | — | Host do RabbitMQ. |
| `RABBITMQ_PORT` | sim | — | Porta AMQP (5672). |
| `RABBITMQ_USER` | sim | — | Usuário do RabbitMQ. |
| `RABBITMQ_PASS` | sim | — | Senha do RabbitMQ. |
| `JWT_SECRET` | uma das duas | — | Segredo compartilhado com o busco-api, codificado em BASE64URL; habilita validação HS256. |
| `JWT_ISSUER` | uma das duas | — | Issuer OAuth2 (resource server) — alternativa ao HS256. |
| `TENANT_ID` | não | `00000000-0000-0000-0000-000000000001` | Tenant fixo da instalação, usado quando o token/mensagem não traz `tenant_id`. |

Configure **`JWT_SECRET` (HS256)** ou **`JWT_ISSUER` (OAuth2)**; sem nenhum dos
dois o serviço não sobe (o `SecurityConfig` falha na criação do `JwtDecoder`).

### Propriedades opcionais (application.properties)

| Propriedade | Default | Descrição |
| --- | --- | --- |
| `planejamento.outbox.poll-interval-ms` | `5000` | Intervalo do publisher da outbox. |
| `planejamento.outbox.batch-size` | `50` | Lote máximo por ciclo da outbox. |
| `planejamento.outbox.backoff-base-ms` | `5000` | Backoff exponencial base em falha de publicação. |
| `planejamento.outbox.backoff-max-ms` | `300000` | Teto do backoff. |
| `planejamento.jobs.auto-processamento.enabled` | `true` | Habilita o job local de confirmação/revalidação de agendamentos. |
| `planejamento.tenant-id` | `${TENANT_ID:...}` | Tenant fixo (mesma função da env `TENANT_ID`). |

## Perfis

- **default** — todos os jobs ativos (inclui auto-processamento local a cada 6h).
- **integration** (`application-integration.properties`) — desliga o
  auto-processamento (`planejamento.jobs.auto-processamento.enabled=false`).
  Use no cluster integrado ao busco-api: os agendamentos nascem apenas do fluxo
  de confirmação de lote comandado por evento.
- **test** — H2 em memória, Flyway desligado, segurança aberta (apenas testes).

## Mensageria (CONTRACT.md §1)

- Consome: exchange `planejamento.exchange` → fila `planejamento.commands.v1`
  (DLQ `planejamento.commands.v1.dlq`).
- Publica: exchange `planejamento.events.exchange` → fila
  `busco-api.planejamento.v1` (DLQ `busco-api.planejamento.v1.dlq`), via outbox
  transacional (at-least-once) e inbox idempotente por `eventId`.
- Headers obrigatórios: `event-type`, `event-version`, `tenant-id`.

## Migrações

Flyway em `classpath:db/migration/planejamento` aplica apenas o schema
`agendamento` (histórico em `agendamento.flyway_schema_history_agendamento`).
