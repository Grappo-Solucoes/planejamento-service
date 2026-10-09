# Manifests K8s de referência — planejamento-service (tarefa 7.2)

Referência para o cluster de terceiros (namespace, pipeline e Ingress são de responsabilidade da
operação). Contém apenas o lado `planejamento-service`; o lado `busco-api` tem seu próprio `k8s/`.

| Arquivo | Conteúdo |
|---|---|
| `deployment.yaml` | Deployment com probes (`/actuator/health/liveness`, `/readiness`) e envs via ConfigMap/Secret |
| `service.yaml` | Service `ClusterIP` (porta 80 → 8080) |
| `configmap.yaml` | Variáveis não sensíveis (perfil, datasource, Flyway, Redis, RabbitMQ, tenant) |
| `secret.example.yaml` | Exemplo de Secret (senha do banco, senha do RabbitMQ e `JWT_SECRET`) — não comitar valores reais |

## Contrato de variáveis de ambiente

Espelha 1:1 os placeholders de `src/main/resources/application.properties` (mais
`SPRING_PROFILES_ACTIVE`/`SPRING_FLYWAY_ENABLED`, exigidos pelo perfil de integração).

| Variável | Origem | Observação |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ConfigMap | `integration` (desliga o auto-processamento local) |
| `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` | ConfigMap / Secret | banco `planejamento`, schema `agendamento` |
| `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT` | ConfigMap | `org.hibernate.dialect.PostgreSQLDialect` |
| `SPRING_FLYWAY_ENABLED` | ConfigMap | `true` (default); Flyway aplica o schema `agendamento` |
| `REDIS_HOST` / `REDIS_PORT` | ConfigMap | Redis |
| `RABBITMQ_HOST` / `PORT` / `USER` / `PASS` | ConfigMap / Secret | topologia do contrato v1 |
| `TENANT_ID` | ConfigMap | default `...0001`; o claim `tenant_id` do token tem precedência |
| `JWT_SECRET` | Secret | HS256 (BASE64URL) compartilhado com o busco-api; alternativa `JWT_ISSUER` |
| `SERVER_PORT` | ConfigMap | `8080` (default do Spring Boot) |

Os nomes seguem os placeholders reais do `application.properties` (`RABBITMQ_USER`/`RABBITMQ_PASS`,
não `RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD`).

### Topologia RabbitMQ (contrato v1 — `docs/events/v1/CONTRACT.md` §1)

Nomes congelados em `RabbitTopologia.java` (não configuráveis por env):

- Consome: exchange `planejamento.exchange` → fila `planejamento.commands.v1`
  (DLQ `planejamento.commands.v1.dlq`).
- Publica: exchange `planejamento.events.exchange` → fila `busco-api.planejamento.v1`
  (DLQ `busco-api.planejamento.v1.dlq`), via outbox transacional.

## Aplicar e validar

```bash
# Validação estática (executada neste repositório; ver resultado abaixo)
kubeconform -strict -summary k8s/

# Equivalente sem kubeconform instalado, via Docker:
docker run --rm -v "$PWD/k8s:/k8s" ghcr.io/yannh/kubeconform:latest -strict -summary /k8s

# Dry-run contra o cluster (namespace e pipeline são de terceiros)
kubectl apply -f k8s/ --dry-run=server
```

### Resultado da validação

`kubectl` não está instalado no ambiente; `kubeconform` foi executado via Docker
(`ghcr.io/yannh/kubeconform:latest`) em modo estrito:

```
Summary: 4 resources found in 4 files - Valid: 4, Invalid: 0, Errors: 0, Skipped: 0
```

## Notas de operação

- **Probes e autenticação**: `management.endpoint.health.probes.enabled=true` e a exposição
  (`management.endpoints.web.exposure.include=health,info,prometheus`) estão em
  `application.properties`. O `SecurityConfig` libera `/actuator/health/**` (`permitAll`), então
  `/actuator/health/liveness` e `/actuator/health/readiness` respondem `200` **sem token** — é o que
  o kubelet usa nestes manifests (sem `httpHeaders`). O restante do Actuator e os endpoints de
  negócio continuam exigindo JWT (service-runtime D6; ver `SegurancaHS256IT`).
- **Métricas (`/actuator/prometheus`)**: mantido **autenticado** (decisão registrada). O scrape
  exige `Authorization: Bearer <token>` (ex.: `bearer_token_file` no Prometheus ou
  `ServiceMonitor`/`ScrapeConfig`) **ou** uma network policy restringindo a porta 8080 ao namespace
  de monitoração. O endpoint só responde se a dependência `micrometer-registry-prometheus` for
  adicionada ao `pom.xml` (a exposição está habilitada, mas sem o registry o path retorna 404 após
  autenticar). Optamos por não expor métricas sem autenticação; se a operação preferir liberá-las
  dentro da rede do cluster, basta incluir `/actuator/prometheus` no `permitAll` do `SecurityConfig`.
- **TLS do Postgres**: `application.properties` fixa
  `spring.datasource.hikari.data-source-properties.sslmode=require`. O Postgres do cluster precisa
  apresentar TLS; caso contrário, sobrescreva com
  `SPRING_DATASOURCE_HIKARI_DATA_SOURCE_PROPERTIES_SSLMODE=disable` no ConfigMap (apenas em
  ambientes sem TLS).
- **Réplicas**: `replicas: 2`. Consumidores competem na mesma fila e o inbox deduplica por
  `eventId`; o publisher da outbox é at-least-once.
- **Segredos**: use External Secrets/Sealed Secrets no pipeline; `secret.example.yaml` é apenas
  ilustrativo.
