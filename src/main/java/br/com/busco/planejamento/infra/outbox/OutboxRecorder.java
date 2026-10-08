package br.com.busco.planejamento.infra.outbox;

import br.com.busco.planejamento.infra.events.DomainEventToIntegrationEventMapper;
import br.com.busco.planejamento.infra.events.EventoJson;
import br.com.busco.planejamento.infra.events.IntegrationEvent;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Grava o evento de integração na outbox na mesma transação do processamento
 * (o listener roda antes do commit). A publicação efetiva é assíncrona
 * (ver {@link OutboxPublisher}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRecorder {

    private final OutboxEventRepository repository;
    private final DomainEventToIntegrationEventMapper mapper;
    private final EventoJson eventoJson;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(DomainEvent evento) {
        mapper.mapear(evento).ifPresent(this::registrar);
    }

    public void registrar(IntegrationEvent evento) {
        LocalDateTime occurredOn = LocalDateTime.ofInstant(evento.occurredOn(), ZoneOffset.UTC);
        OutboxEvent registro = OutboxEvent.builder()
                .id(UUID.randomUUID().toString())
                .eventId(evento.eventId())
                .eventType(evento.eventType())
                .version(evento.version())
                .occurredOn(occurredOn)
                .correlationId(evento.correlationId())
                .tenantId(evento.tenantId())
                .payload(eventoJson.paraJson(evento.payload()))
                .status(OutboxStatus.PENDENTE)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(registro);
        log.debug("Evento {} gravado na outbox (eventId={})", evento.eventType(), evento.eventId());
    }
}
