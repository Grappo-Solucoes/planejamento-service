package br.com.busco.planejamento.infra.events;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Envelope recebido (comandos), conforme CONTRACT §2.
 */
public record EnvelopeEvento(
        String eventId,
        String eventType,
        String version,
        Instant occurredOn,
        String correlationId,
        String tenantId,
        JsonNode payload
) {
}
