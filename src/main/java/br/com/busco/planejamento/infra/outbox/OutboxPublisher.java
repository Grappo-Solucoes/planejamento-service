package br.com.busco.planejamento.infra.outbox;

import br.com.busco.planejamento.infra.events.EventoJson;
import br.com.busco.planejamento.infra.rabbit.RabbitTopologia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Publisher assíncrono da outbox: poll a cada 5s (configurável), lote de 50,
 * backoff exponencial entre tentativas. A mensagem sai como {@code String}
 * JSON pré-serializada, com headers {@code event-type}/{@code event-version}/
 * {@code tenant-id} e correlationId na propriedade AMQP (CONTRACT §2).
 */
@Component
@ConditionalOnProperty(name = "planejamento.outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int TAMANHO_MAXIMO_ERRO = 2000;

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final EventoJson eventoJson;

    @Value("${planejamento.outbox.batch-size:50}")
    private int batchSize;

    @Value("${planejamento.outbox.backoff-base-ms:5000}")
    private long backoffBaseMs;

    @Value("${planejamento.outbox.backoff-max-ms:300000}")
    private long backoffMaxMs;

    @Scheduled(fixedDelayString = "${planejamento.outbox.poll-interval-ms:5000}")
    public void publicarPendentes() {
        List<OutboxEvent> pendentes = repository.buscarPendentes(
                OutboxStatus.PENDENTE, LocalDateTime.now(), PageRequest.of(0, batchSize));
        if (!pendentes.isEmpty()) {
            log.debug("Publicando {} evento(s) da outbox", pendentes.size());
        }
        pendentes.forEach(this::publicar);
    }

    public void publicar(OutboxEvent registro) {
        try {
            String envelope = eventoJson.envelope(registro);
            rabbitTemplate.convertAndSend(RabbitTopologia.EVENTS_EXCHANGE, registro.getEventType(), envelope, message -> {
                MessageProperties props = message.getMessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                props.setContentEncoding("UTF-8");
                props.setHeader("event-type", registro.getEventType());
                props.setHeader("event-version", registro.getVersion());
                props.setHeader("tenant-id", registro.getTenantId());
                props.setCorrelationId(registro.getCorrelationId());
                return message;
            });
            registro.setStatus(OutboxStatus.PUBLICADO);
            registro.setPublishedAt(LocalDateTime.now());
            registro.setLastError(null);
            repository.save(registro);
            log.debug("Evento publicado: {} (eventId={})", registro.getEventType(), registro.getEventId());
        } catch (Exception e) {
            log.error("Falha ao publicar evento {} (eventId={}); reagendando",
                    registro.getEventType(), registro.getEventId(), e);
            registro.setAttempts(registro.getAttempts() + 1);
            registro.setNextAttemptAt(LocalDateTime.now().plusNanos(backoff(registro.getAttempts()) * 1_000_000L));
            registro.setLastError(erroResumido(e));
            repository.save(registro);
        }
    }

    private long backoff(int tentativas) {
        long valor = backoffBaseMs;
        for (int i = 1; i < tentativas; i++) {
            valor = Math.min(valor * 2, backoffMaxMs);
        }
        return Math.min(valor, backoffMaxMs);
    }

    private String erroResumido(Exception e) {
        String mensagem = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return mensagem.length() > TAMANHO_MAXIMO_ERRO ? mensagem.substring(0, TAMANHO_MAXIMO_ERRO) : mensagem;
    }
}
