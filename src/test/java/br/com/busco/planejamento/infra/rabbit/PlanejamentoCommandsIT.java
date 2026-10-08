package br.com.busco.planejamento.infra.rabbit;

import br.com.busco.planejamento.app.PlanejamentoService;
import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.domain.StatusAgendamento;
import br.com.busco.planejamento.infra.events.EventoJson;
import br.com.busco.planejamento.infra.events.EventosV1;
import br.com.busco.planejamento.infra.inbox.ProcessedEventRepository;
import br.com.busco.planejamento.infra.outbox.OutboxEvent;
import br.com.busco.planejamento.infra.outbox.OutboxEventRepository;
import br.com.busco.planejamento.infra.outbox.OutboxPublisher;
import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * IT de ponta a ponta dos comandos do catálogo v1 (CONTRACT §4/§5): publica o
 * comando na exchange de comandos, aguarda o processamento pelo listener real e
 * confere o evento de resultado na fila do busco-api, passando pela outbox.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Comandos v1 - Testes de Integração")
class PlanejamentoCommandsIT {

    private static final String TENANT = "00000000-0000-0000-0000-000000000001";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter FORMATO_INSTANTE_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private EventoJson eventoJson;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private PlanejamentoService planejamentoService;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    private RotaId rotaId;
    private VeiculoId veiculoId;
    private MotoristaId motoristaId;
    private LocalDateTime inicio;
    private LocalDateTime fim;

    @BeforeEach
    void setUp() {
        rotaId = RotaId.randomId();
        veiculoId = VeiculoId.randomId();
        motoristaId = MotoristaId.randomId();
        LocalDate primeiraSegunda = LocalDate.now().plusDays(14)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        inicio = LocalDateTime.of(primeiraSegunda, LocalTime.of(10, 0));
        fim = inicio.plusDays(6);
    }

    @Test
    @DisplayName("criar.solicitado gera lote.criado com o mesmo UUID e payload completo")
    void criarGeraLoteCriado() {
        PlanejamentoLoteId loteId = PlanejamentoLoteId.randomId();

        publicar(EventosV1.LOTE_CRIAR_SOLICITADO, loteId, payloadLote(loteId, rotaId, Set.of(1, 3, 5)));

        List<OutboxEvent> eventos = aguardarResultado(loteId, EventosV1.LOTE_FALHA, "planejamento.lote.criado", "planejamento.lote.criado");
        assertThat(eventos).extracting(OutboxEvent::getEventType).containsExactly("planejamento.lote.criado");

        publicarOutbox();
        JsonNode evento = eventoPorTipo(receberEventosDoLote(loteId, eventos.size()), "planejamento.lote.criado");

        assertEnvelope(evento, "planejamento.lote.criado", loteId.toUUID());
        JsonNode payload = evento.path("payload");
        assertThat(payload.path("loteId").asText()).isEqualTo(loteId.toUUID());
        assertThat(payload.path("status").asText()).isEqualTo("RASCUNHO");
        assertThat(payload.path("rotaId").asText()).isEqualTo(rotaId.toUUID());
        assertThat(payload.path("veiculoId").asText()).isEqualTo(veiculoId.toUUID());
        assertThat(payload.path("motoristaId").asText()).isEqualTo(motoristaId.toUUID());
        assertThat(payload.path("diasDaSemana")).extracting(JsonNode::asInt).containsExactly(1, 3, 5);

        // mesmo UUID do busco-api
        assertThat(planejamentoService.buscarPorId(loteId).getId()).isEqualTo(loteId);
    }

    @Test
    @DisplayName("confirmar.solicitado gera agendamento.criado por data e lote.confirmado com a contagem")
    void confirmarGeraAgendamentosEConfirmacao() {
        PlanejamentoLoteId loteId = criarLote();
        publicar(EventosV1.LOTE_CONFIRMAR_SOLICITADO, loteId, Map.of("loteId", loteId.toUUID()));

        List<OutboxEvent> eventos = aguardarResultado(loteId, EventosV1.LOTE_FALHA, "planejamento.lote.confirmado", "planejamento.lote.confirmado");
        Map<String, Long> porTipo = contarPorTipo(eventos);
        assertThat(porTipo).containsEntry("agendamento.criado", 3L);

        publicarOutbox();
        var recebidos = receberEventosDoLote(loteId, eventos.size());
        JsonNode confirmado = eventoPorTipo(recebidos, "planejamento.lote.confirmado");
        assertThat(confirmado.path("payload").path("loteId").asText()).isEqualTo(loteId.toUUID());
        assertThat(confirmado.path("payload").path("quantidadeAgendamentos").asInt()).isEqualTo(3);

        JsonNode agendamentoCriado = eventoPorTipo(recebidos, "agendamento.criado");
        assertThat(agendamentoCriado.path("payload").path("loteId").asText()).isEqualTo(loteId.toUUID());
        assertThat(agendamentoCriado.path("payload").path("rotaId").asText()).isEqualTo(rotaId.toUUID());

        assertThat(agendamentosDoLote(loteId)).hasSize(3);
    }

    @Test
    @DisplayName("editar.solicitado cancela futuros, re-expande e publica lote.editado")
    void editarCancelaEReExpande() {
        PlanejamentoLoteId loteId = criarLote();
        publicar(EventosV1.LOTE_CONFIRMAR_SOLICITADO, loteId, Map.of("loteId", loteId.toUUID()));
        aguardarResultado(loteId, EventosV1.LOTE_FALHA, "planejamento.lote.confirmado", "planejamento.lote.confirmado");

        RotaId rotaNova = RotaId.randomId();
        publicar(EventosV1.LOTE_EDITAR_SOLICITADO, loteId, payloadLote(loteId, rotaNova, Set.of(5)));

        List<OutboxEvent> eventos = aguardarResultado(loteId, EventosV1.LOTE_FALHA, "planejamento.lote.editado", "planejamento.lote.editado");
        Map<String, Long> porTipo = contarPorTipo(eventos);
        assertThat(porTipo).containsEntry("agendamento.cancelado", 3L);
        assertThat(porTipo).containsEntry("agendamento.criado", 4L); // 3 da confirmação + 1 da re-expansão

        publicarOutbox();
        var recebidos = receberEventosDoLote(loteId, eventos.size());
        JsonNode editado = eventoPorTipo(recebidos, "planejamento.lote.editado");
        assertThat(editado.path("payload").path("rotaId").asText()).isEqualTo(rotaNova.toUUID());

        JsonNode cancelado = eventoPorTipo(recebidos, "agendamento.cancelado");
        assertThat(cancelado.path("payload").path("motivo").asText()).isEqualTo("Cancelado por edição");

        var agendamentos = agendamentosDoLote(loteId);
        assertThat(agendamentos.stream().filter(a -> a.getRota().equals(rotaNova)).toList()).hasSize(1);
        assertThat(agendamentos.stream().filter(a -> a.getStatus() == StatusAgendamento.CANCELADO)).hasSize(3);
    }

    @Test
    @DisplayName("cancelar.solicitado cancela futuros e publica lote.cancelado com a contagem")
    void cancelarGeraLoteCancelado() {
        PlanejamentoLoteId loteId = criarLote();
        publicar(EventosV1.LOTE_CONFIRMAR_SOLICITADO, loteId, Map.of("loteId", loteId.toUUID()));
        aguardarResultado(loteId, EventosV1.LOTE_FALHA, "planejamento.lote.confirmado", "planejamento.lote.confirmado");

        publicar(EventosV1.LOTE_CANCELAR_SOLICITADO, loteId, Map.of("loteId", loteId.toUUID()));

        List<OutboxEvent> eventos = aguardarResultado(loteId, EventosV1.LOTE_FALHA, "planejamento.lote.cancelado", "planejamento.lote.cancelado");
        assertThat(contarPorTipo(eventos)).containsEntry("agendamento.cancelado", 3L);

        publicarOutbox();
        JsonNode cancelado = eventoPorTipo(receberEventosDoLote(loteId, eventos.size()), "planejamento.lote.cancelado");
        assertThat(cancelado.path("payload").path("loteId").asText()).isEqualTo(loteId.toUUID());
        assertThat(cancelado.path("payload").path("quantidadeAgendamentosCancelados").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("Comando rejeitado publica lote.falha com o motivo")
    void comandoRejeitadoGeraFalha() {
        PlanejamentoLoteId inexistente = PlanejamentoLoteId.randomId();

        publicar(EventosV1.LOTE_CONFIRMAR_SOLICITADO, inexistente, Map.of("loteId", inexistente.toUUID()));

        List<OutboxEvent> eventos = aguardarResultado(inexistente, EventosV1.LOTE_FALHA, "planejamento.lote.falha", "planejamento.lote.falha");
        assertThat(eventos).extracting(OutboxEvent::getEventType).containsExactly("planejamento.lote.falha");

        publicarOutbox();
        JsonNode falha = eventoPorTipo(receberEventosDoLote(inexistente, eventos.size()), "planejamento.lote.falha");
        assertThat(falha.path("payload").path("loteId").asText()).isEqualTo(inexistente.toUUID());
        assertThat(falha.path("payload").path("comando").asText()).isEqualTo(EventosV1.LOTE_CONFIRMAR_SOLICITADO);
        assertThat(falha.path("payload").path("motivo").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Reentrega do mesmo eventId confirma sem duplicar efeito (inbox)")
    void reentregaDoMesmoComandoNaoDuplicaEfeito() {
        PlanejamentoLoteId loteId = PlanejamentoLoteId.randomId();
        String eventId = UUID.randomUUID().toString();

        publicar(EventosV1.LOTE_CRIAR_SOLICITADO, loteId, payloadLote(loteId, rotaId, Set.of(1, 3, 5)), eventId);
        List<OutboxEvent> eventos = aguardarResultado(loteId, EventosV1.LOTE_FALHA, "planejamento.lote.criado", "planejamento.lote.criado");
        assertThat(eventos).hasSize(1);
        assertThat(processedEventRepository.findById(eventId)).isPresent();

        // reentrega com o mesmo eventId: ack sem efeito
        publicar(EventosV1.LOTE_CRIAR_SOLICITADO, loteId, payloadLote(loteId, rotaId, Set.of(1, 3, 5)), eventId);
        aguardarConsumoDosComandos();

        assertThat(outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId.toUUID())).hasSize(1);
        assertThat(processedEventRepository.findById(eventId)).isPresent();
        assertThat(planejamentoService.buscarPorId(loteId).getId()).isEqualTo(loteId);
    }

    // --- helpers ---

    private PlanejamentoLoteId criarLote() {
        PlanejamentoLoteId loteId = PlanejamentoLoteId.randomId();
        publicar(EventosV1.LOTE_CRIAR_SOLICITADO, loteId, payloadLote(loteId, rotaId, Set.of(1, 3, 5)));
        aguardarResultado(loteId, EventosV1.LOTE_FALHA, "planejamento.lote.criado", "planejamento.lote.criado");
        return loteId;
    }

    private Map<String, Object> payloadLote(PlanejamentoLoteId loteId, RotaId rota, Set<Integer> dias) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loteId", loteId.toUUID());
        payload.put("rotaId", rota.toUUID());
        payload.put("veiculoId", veiculoId.toUUID());
        payload.put("motoristaId", motoristaId.toUUID());
        payload.put("dataInicio", inicio);
        payload.put("dataFim", fim);
        payload.put("diasDaSemana", List.copyOf(dias));
        return payload;
    }

    private void publicar(String eventType, PlanejamentoLoteId loteId, Map<String, Object> payload) {
        publicar(eventType, loteId, payload, UUID.randomUUID().toString());
    }

    private void publicar(String eventType, PlanejamentoLoteId loteId, Map<String, Object> payload, String eventId) {
        ObjectNode envelope = eventoJson.mapper().createObjectNode();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("version", EventosV1.VERSAO);
        envelope.put("occurredOn", FORMATO_INSTANTE_UTC.format(Instant.now()));
        envelope.put("correlationId", loteId.toUUID());
        envelope.put("tenantId", TENANT);
        envelope.set("payload", eventoJson.mapper().valueToTree(payload));

        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        props.setHeader("event-type", eventType);
        props.setHeader("event-version", EventosV1.VERSAO);
        props.setHeader("tenant-id", TENANT);
        props.setCorrelationId(loteId.toUUID());

        rabbitTemplate.send(RabbitTopologia.COMMANDS_EXCHANGE, eventType,
                new Message(envelope.toString().getBytes(StandardCharsets.UTF_8), props));
    }

    private void publicarOutbox() {
        outboxPublisher.publicarPendentes();
    }

    private List<OutboxEvent> aguardarResultado(
            PlanejamentoLoteId loteId, String eventTypeFalha, String eventTypeFinal, String descricao) {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        List<OutboxEvent> eventos = List.of();
        while (System.nanoTime() < deadline) {
            eventos = outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId.toUUID());
            boolean concluido = eventos.stream().anyMatch(e -> eventTypeFinal.equals(e.getEventType()));
            boolean falhou = eventos.stream().anyMatch(e -> eventTypeFalha.equals(e.getEventType()));
            if (concluido || falhou) {
                return eventos;
            }
            dormir(100);
        }
        fail("Timeout aguardando %s do lote %s; outbox: %s", descricao, loteId.toUUID(),
                eventos.stream().map(OutboxEvent::getEventType).toList());
        return eventos;
    }

    private List<JsonNode> receberEventosDoLote(PlanejamentoLoteId loteId, int quantidadeEsperada) {
        List<JsonNode> recebidos = new ArrayList<>();
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline && recebidos.size() < quantidadeEsperada) {
            Message mensagem = rabbitTemplate.receive(RabbitTopologia.EVENTS_QUEUE, 500);
            if (mensagem == null) {
                continue;
            }
            JsonNode evento = lerJson(mensagem);
            if (loteId.toUUID().equals(evento.path("correlationId").asText())) {
                recebidos.add(evento);
            }
        }
        assertThat(recebidos)
                .withFailMessage("Esperava %s eventos do lote %s na fila %s, mas recebi %s: %s",
                        quantidadeEsperada, loteId.toUUID(), RabbitTopologia.EVENTS_QUEUE, recebidos.size(),
                        recebidos.stream().map(e -> e.path("eventType").asText()).toList())
                .hasSize(quantidadeEsperada);
        return recebidos;
    }

    private JsonNode eventoPorTipo(List<JsonNode> eventos, String eventType) {
        return eventos.stream()
                .filter(evento -> eventType.equals(evento.path("eventType").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Evento " + eventType + " não recebido: "
                        + eventos.stream().map(e -> e.path("eventType").asText()).toList()));
    }

    private JsonNode lerJson(Message mensagem) {
        try {
            return eventoJson.mapper().readTree(mensagem.getBody());
        } catch (IOException e) {
            throw new IllegalStateException("Evento recebido não é JSON válido", e);
        }
    }

    private void assertEnvelope(JsonNode evento, String eventType, String correlationId) {
        assertThat(evento.path("eventId").asText()).isNotBlank();
        assertThat(evento.path("eventType").asText()).isEqualTo(eventType);
        assertThat(evento.path("version").asText()).isEqualTo("1.0");
        assertThat(evento.path("occurredOn").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat(evento.path("correlationId").asText()).isEqualTo(correlationId);
        assertThat(evento.path("tenantId").asText()).isEqualTo(TENANT);
    }

    private Map<String, Long> contarPorTipo(List<OutboxEvent> eventos) {
        return eventos.stream().collect(Collectors.groupingBy(OutboxEvent::getEventType, Collectors.counting()));
    }

    private List<AgendamentoOperacional> agendamentosDoLote(PlanejamentoLoteId loteId) {
        return agendamentoRepository
                .buscarIdFuturosPorPlanejamento(loteId, LocalDateTime.of(2023, 12, 31, 23, 59))
                .stream()
                .map(id -> agendamentoRepository.findById(id).orElseThrow())
                .toList();
    }

    private void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido aguardando resultado", e);
        }
    }

    private void aguardarConsumoDosComandos() {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (mensagensNaFila(RabbitTopologia.COMMANDS_QUEUE) == 0) {
                // margem para o processamento assíncrono do listener terminar
                dormir(1000);
                return;
            }
            dormir(100);
        }
        fail("Fila de comandos não foi consumida dentro do timeout");
    }

    private long mensagensNaFila(String fila) {
        Long total = rabbitTemplate.execute(channel -> channel.messageCount(fila));
        return total == null ? 0 : total;
    }
}
