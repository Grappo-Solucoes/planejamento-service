# Contrato de eventos v1 — planejamento (congelado)

Este documento congela a interface entre `busco-api` e `planejamento-service` para a v1.
Fonte de verdade: `openspec/changes/integrate-planejamento-service/specs/planejamento/event-contract/spec.md`.
Mudanças incompatíveis exigem nova versão de evento; mudanças aditivas são permitidas.

## 1. Topologia RabbitMQ

| Elemento | Nome |
|---|---|
| Exchange de comandos (topic) | `planejamento.exchange` |
| Fila de comandos (durável) | `planejamento.commands.v1` — consumida pelo serviço |
| DLQ de comandos | `planejamento.commands.v1.dlq` |
| Exchange de eventos (topic) | `planejamento.events.exchange` |
| Fila de eventos (durável) | `busco-api.planejamento.v1` — consumida pelo `busco-api` |
| DLQ de eventos | `busco-api.planejamento.v1.dlq` |
| Dead-letter exchange | `planejamento.dlx` (topic) |

Regras:
- Routing key = `eventType` (ex.: `planejamento.lote.criar.solicitado`).
- Cada exchange é bindada à sua fila com `#`.
- Cada fila principal declara `x-dead-letter-exchange=planejamento.dlx` e
  `x-dead-letter-routing-key=<nome-da-fila>.dlq`; cada DLQ é bindada ao `planejamento.dlx` pela mesma routing key.
- JSON puro: produtores publicam `String` (JSON já serializado) com `SimpleMessageConverter` e
  `content_type=application/json`; consumidores recebem `String` e desserializam com Jackson.
  **Não** usar cabeçalho `__TypeId__` (sem acoplamento a classes Java).
- Retry do consumidor: 3 tentativas (1s, 2s, 4s; backoff exponencial, máx. 10s), depois `reject` → DLQ.
- Outbox: poll a cada 5s, lote de 50, retry com backoff; entrega ao menos uma vez.
- Inbox: processar cada `eventId` no máximo uma vez (tabela de eventos processados).

## 2. Envelope (toda mensagem)

```json
{
  "eventId": "uuid",
  "eventType": "planejamento.lote.criar.solicitado",
  "version": "1.0",
  "occurredOn": "2026-10-08T19:00:00Z",
  "correlationId": "uuid-do-lote",
  "tenantId": "uuid",
  "payload": { }
}
```

Headers AMQP obrigatórios: `event-type` (= `eventType`), `event-version` (= `1.0`), `tenant-id` (= `tenantId`).
A propriedade AMQP `correlationId` também é preenchida com o `correlationId`.

## 3. Convenções de payload

- `uuid`: string no formato canônico (`8-4-4-4-12`, minúsculas).
- `localDateTime`: string `yyyy-MM-dd'T'HH:mm:ss` **sem offset** (ambos os serviços no mesmo fuso).
  Mesmo horário em todas as datas do lote (hora da viagem).
- `diasDaSemana`: array de inteiros `1..7` (ISO-8601: 1=segunda … 7=domingo), sem repetição.
- `occurredOn`: instante UTC ISO-8601 com `Z` (ex.: `2026-10-08T19:00:00Z`).
- `correlationId` = `loteId` = UUID do `Planejamento` local do `busco-api`.
- Tenant da instalação (busco-api): `app.planejamento.tenant-id`, default `00000000-0000-0000-0000-000000000001`;
  no serviço, `TENANT_ID` com o mesmo valor.

## 4. Catálogo v1

### 4.1 Comandos (busco-api → serviço)

**`planejamento.lote.criar.solicitado`**
```json
{ "loteId": "uuid", "rotaId": "uuid", "veiculoId": "uuid", "motoristaId": "uuid",
  "dataInicio": "localDateTime", "dataFim": "localDateTime", "diasDaSemana": [1,3,5] }
```
Mapeamento no busco-api: `dataInicio = planejamento.dataInicio.atTime(horaViagem)`,
`dataFim = planejamento.dataFim.atTime(horaViagem)`.

**`planejamento.lote.editar.solicitado`** — mesmo payload do criar (substituição completa dos campos de agenda).

**`planejamento.lote.confirmar.solicitado`**
```json
{ "loteId": "uuid" }
```

**`planejamento.lote.cancelar.solicitado`**
```json
{ "loteId": "uuid" }
```

### 4.2 Eventos de resultado (serviço → busco-api)

**`planejamento.lote.criado`**
```json
{ "loteId": "uuid", "status": "RASCUNHO", "rotaId": "uuid", "veiculoId": "uuid", "motoristaId": "uuid",
  "dataInicio": "localDateTime", "dataFim": "localDateTime", "diasDaSemana": [1,3,5] }
```

**`planejamento.lote.editado`** — mesmo payload do criado, com os novos valores (sem `status`).

**`planejamento.lote.confirmado`**
```json
{ "loteId": "uuid", "quantidadeAgendamentos": 12 }
```

**`planejamento.lote.cancelado`**
```json
{ "loteId": "uuid", "quantidadeAgendamentosCancelados": 3 }
```

**`planejamento.lote.falha`**
```json
{ "loteId": "uuid", "comando": "planejamento.lote.criar.solicitado", "motivo": "descrição" }
```

**`agendamento.criado`**
```json
{ "agendamentoId": "uuid", "loteId": "uuid", "data": "localDateTime",
  "rotaId": "uuid", "veiculoId": "uuid", "motoristaId": "uuid" }
```

**`agendamento.cancelado`**
```json
{ "agendamentoId": "uuid", "loteId": "uuid", "data": "localDateTime", "motivo": "descrição" }
```

## 5. Semântica de resultado

- Todo comando do catálogo produz exatamente um evento de resultado: o evento de sucesso correspondente
  (`criado`/`editado`/`confirmado`/`cancelado`) ou `planejamento.lote.falha` com o motivo.
- `confirmado.quantidadeAgendamentos` = agendamentos criados naquela confirmação (pode ser 0).
- `cancelado.quantidadeAgendamentosCancelados` = agendamentos futuros cancelados.
- Na edição, o serviço cancela os agendamentos futuros não `REALIZADO` (emitindo `agendamento.cancelado`
  para cada um) e re-expande (emitindo `agendamento.criado` para cada nova data); depois publica `lote.editado`.
- `agendamento.cancelado.motivo`: `"Cancelado por edição"` (edição) ou `"Cancelado por planejamento"` (cancelamento do lote).

## 6. Regras de idempotência e correlação

- Consumidores registram `eventId` processado; reentrega é confirmada sem efeito.
- `eventId` e `correlationId` não mudam em republicação da outbox.
- Mensagem sem `eventId`, `eventType` ou fora do catálogo: rejeitar para a DLQ sem processamento.
- `planejamento.lote.falha` não volta para a DLQ; é o resultado de negócio.
