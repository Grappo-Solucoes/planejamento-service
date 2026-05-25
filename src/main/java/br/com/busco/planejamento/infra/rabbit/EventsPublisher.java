package br.com.busco.planejamento.infra.rabbit;

import br.com.busco.planejamento.sk.ddd.DomainEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class EventsPublisher {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${viagem-exchange}")
    private String exchange;

    private void publicarEvento(Object evento, String eventType) {
        try {
            log.info("Publicando evento {} ",
                    eventType);

            rabbitTemplate.convertAndSend(exchange, "", evento,
                    message -> {
                        message.getMessageProperties().setHeader("event-type", eventType);
                        message.getMessageProperties().setHeader("version", "1.0");
                        message.getMessageProperties().setHeader("timestamp", System.currentTimeMillis());
                        return message;
                    });

            log.info("Evento publicado com sucesso: {}", eventType);

        } catch (Exception e) {
            log.error("Erro ao publicar evento: {}", eventType, e);
            throw new RuntimeException("Falha ao publicar evento", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(@NonNull DomainEvent domainEvent) {
        publicarEvento(domainEvent, domainEvent.getClass().getSimpleName());
    }

}
