package br.com.busco.planejamento.infra.rabbit;

import br.com.busco.planejamento.infra.events.DomainEventToIntegrationEventMapper;
import br.com.busco.planejamento.infra.events.EventoJson;
import br.com.busco.planejamento.infra.outbox.OutboxEvent;
import br.com.busco.planejamento.infra.outbox.OutboxEventRepository;
import br.com.busco.planejamento.infra.outbox.OutboxPublisher;
import br.com.busco.planejamento.infra.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica a topologia do CONTRACT §1 em broker real (Testcontainers):
 * comando roteia para {@code planejamento.commands.v1}; evento de resultado
 * publicado pela outbox roteia para {@code busco-api.planejamento.v1}; falha
 * permanente é encaminhada para a DLQ dedicada.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RabbitMQTopologiaIT {

    private static final String TENANT = "00000000-0000-0000-0000-000000000001";

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-alpine");

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    OutboxPublisher outboxPublisher;

    @Autowired
    EventoJson eventoJson;

    @Test
    void comandoRoteiaParaFilaDeComandosDoServico() {
        String loteId = UUID.randomUUID().toString();
        String corpo = "{\"loteId\":\"" + loteId + "\"}";

        rabbitTemplate.convertAndSend(RabbitTopologia.COMMANDS_EXCHANGE, "planejamento.lote.criar.solicitado",
                corpo, message -> {
                    MessageProperties props = message.getMessageProperties();
                    props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    props.setHeader("event-type", "planejamento.lote.criar.solicitado");
                    props.setHeader("event-version", "1.0");
                    props.setHeader("tenant-id", TENANT);
                    props.setCorrelationId(loteId);
                    return message;
                });

        Message recebida = rabbitTemplate.receive(RabbitTopologia.COMMANDS_QUEUE, 10_000);

        assertThat(recebida).isNotNull();
        assertThat(new String(recebida.getBody(), StandardCharsets.UTF_8)).isEqualTo(corpo);
        assertThat(recebida.getMessageProperties().getHeaders())
                .containsEntry("event-type", "planejamento.lote.criar.solicitado")
                .containsEntry("tenant-id", TENANT)
                .doesNotContainKey("__TypeId__");
        assertThat(recebida.getMessageProperties().getCorrelationId()).isEqualTo(loteId);
    }

    @Test
    void eventoDaOutboxRoteiaParaFilaDoBuscoApi() throws Exception {
        String loteId = UUID.randomUUID().toString();
        OutboxEvent registro = OutboxEvent.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .eventType(DomainEventToIntegrationEventMapper.TIPO_LOTE_CRIADO)
                .version("1.0")
                .occurredOn(LocalDateTime.now(ZoneOffset.UTC).withNano(0))
                .correlationId(loteId)
                .tenantId(TENANT)
                .payload("{\"loteId\":\"" + loteId + "\",\"status\":\"RASCUNHO\"}")
                .status(OutboxStatus.PENDENTE)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();
        outboxEventRepository.save(registro);

        outboxPublisher.publicarPendentes();

        Message recebida = rabbitTemplate.receive(RabbitTopologia.EVENTS_QUEUE, 10_000);
        assertThat(recebida).isNotNull();

        String corpo = new String(recebida.getBody(), StandardCharsets.UTF_8);
        JsonNode envelope = eventoJson.mapper().readTree(corpo);
        assertThat(envelope.get("eventId").asText()).isEqualTo(registro.getEventId());
        assertThat(envelope.get("eventType").asText()).isEqualTo(DomainEventToIntegrationEventMapper.TIPO_LOTE_CRIADO);
        assertThat(envelope.get("version").asText()).isEqualTo("1.0");
        assertThat(envelope.get("occurredOn").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat(envelope.get("correlationId").asText()).isEqualTo(loteId);
        assertThat(envelope.get("tenantId").asText()).isEqualTo(TENANT);
        assertThat(envelope.get("payload").get("loteId").asText()).isEqualTo(loteId);

        assertThat(recebida.getMessageProperties().getHeaders())
                .containsEntry("event-type", DomainEventToIntegrationEventMapper.TIPO_LOTE_CRIADO)
                .containsEntry("event-version", "1.0")
                .containsEntry("tenant-id", TENANT)
                .doesNotContainKey("__TypeId__");
        assertThat(recebida.getMessageProperties().getCorrelationId()).isEqualTo(loteId);

        OutboxEvent atualizado = outboxEventRepository.findById(registro.getId()).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(OutboxStatus.PUBLICADO);
        assertThat(atualizado.getPublishedAt()).isNotNull();
    }

    @Test
    void mensagemRejeitadaVaiParaDlq() {
        String corpo = "{\"invalido\":true}";
        rabbitTemplate.convertAndSend(RabbitTopologia.COMMANDS_EXCHANGE, "planejamento.lote.criar.solicitado",
                corpo, message -> {
                    message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    return message;
                });

        rabbitTemplate.execute(channel -> {
            com.rabbitmq.client.GetResponse resposta = channel.basicGet(RabbitTopologia.COMMANDS_QUEUE, false);
            assertThat(resposta).isNotNull();
            channel.basicNack(resposta.getEnvelope().getDeliveryTag(), false, false);
            return null;
        });

        Message naDlq = rabbitTemplate.receive(RabbitTopologia.COMMANDS_DLQ, 10_000);
        assertThat(naDlq).isNotNull();
        assertThat(new String(naDlq.getBody(), StandardCharsets.UTF_8)).isEqualTo(corpo);
    }
}
