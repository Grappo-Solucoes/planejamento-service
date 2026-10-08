package br.com.busco.planejamento.infra.events;

import br.com.busco.planejamento.infra.outbox.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Serialização/desserialização JSON dos eventos do contrato v1.
 *
 * <p>O contrato congelado exige {@code localDateTime} como
 * {@code yyyy-MM-dd'T'HH:mm:ss} (sem offset) e {@code occurredOn} como instante
 * UTC ISO-8601 com {@code Z}. Os formatos são fixados aqui para não depender do
 * comportamento default do Jackson (que omite segundos quando zerados).</p>
 */
@Component
public class EventoJson {

    static final DateTimeFormatter FORMATO_LOCAL_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    static final DateTimeFormatter FORMATO_INSTANTE_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final ObjectMapper mapper;

    public EventoJson() {
        this.mapper = new ObjectMapper();
        JavaTimeModule javaTime = new JavaTimeModule();
        javaTime.addSerializer(java.time.LocalDateTime.class, new LocalDateTimeSerializer(FORMATO_LOCAL_DATE_TIME));
        javaTime.addDeserializer(java.time.LocalDateTime.class, new LocalDateTimeDeserializer(FORMATO_LOCAL_DATE_TIME));
        this.mapper.registerModule(javaTime);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public String paraJson(Object valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar JSON de evento", e);
        }
    }

    public <T> T deJson(String json, Class<T> tipo) {
        try {
            return mapper.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON inválido para " + tipo.getSimpleName(), e);
        }
    }

    /**
     * Monta o envelope completo de um registro da outbox, reaproveitando o
     * payload já serializado (o mesmo bytes é reusado em republicações).
     */
    public String envelope(OutboxEvent registro) {
        ObjectNode envelope = mapper.createObjectNode();
        envelope.put("eventId", registro.getEventId());
        envelope.put("eventType", registro.getEventType());
        envelope.put("version", registro.getVersion());
        envelope.put("occurredOn", FORMATO_INSTANTE_UTC.format(registro.getOccurredOn()));
        envelope.put("correlationId", registro.getCorrelationId());
        envelope.put("tenantId", registro.getTenantId());
        try {
            envelope.set("payload", mapper.readTree(registro.getPayload()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Payload da outbox inválido em " + registro.getId(), e);
        }
        return envelope.toString();
    }
}
