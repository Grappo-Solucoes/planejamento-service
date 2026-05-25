package br.com.busco.planejamento.sk.ddd;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

@Component
public class TenantAwareMessagePostProcessor implements MessagePostProcessor {

    @Override
    public Message postProcessMessage(Message message) {

        String tenant = message.getMessageProperties()
                .getHeader("tenant-id");

        if (tenant != null) {
            TenantContext.set(TenantId.fromString(tenant));
        }

        return message;
    }
}