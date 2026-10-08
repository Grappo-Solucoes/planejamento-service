package br.com.busco.planejamento.infra.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de integração no envelope versionado do contrato v1
 * (docs/events/v1/CONTRACT.md).
 */
public record IntegrationEvent(
        String eventId,
        String eventType,
        String version,
        Instant occurredOn,
        String correlationId,
        String tenantId,
        Map<String, Object> payload
) {

    public static final String VERSAO_V1 = "1.0";

    public static IntegrationEvent of(
            String eventType,
            Instant occurredOn,
            String correlationId,
            String tenantId,
            Map<String, Object> payload
    ) {
        return new IntegrationEvent(
                UUID.randomUUID().toString(),
                eventType,
                VERSAO_V1,
                occurredOn,
                correlationId,
                tenantId,
                payload
        );
    }
}
