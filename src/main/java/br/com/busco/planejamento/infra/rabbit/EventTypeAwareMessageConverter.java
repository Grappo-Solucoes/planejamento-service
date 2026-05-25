package br.com.busco.planejamento.infra.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventTypeAwareMessageConverter extends Jackson2JsonMessageConverter {

    private final Map<String, Class<?>> eventTypeMapping = Map.of(
//            "ViagemCriada", ViagemCriada.class,
//            "ViagemFinalizada", ViagemFinalizada.class
    );

    public EventTypeAwareMessageConverter() {
        super(customObjectMapper());
    }

    private static ObjectMapper customObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public Object fromMessage(Message message) {
        String eventType = message.getMessageProperties()
                .getHeader("event-type");

        if (eventType == null) {
            return super.fromMessage(message);
        }

        Class<?> clazz = eventTypeMapping.get(eventType);

        if (clazz == null) {
            return super.fromMessage(message);
        }

        try {
            return objectMapper.readValue(message.getBody(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Erro convertendo evento: " + eventType, e);
        }
    }
}