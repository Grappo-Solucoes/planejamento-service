package br.com.busco.planejamento.sk.ddd;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

@Component
public class TenantAwareMessagePostProcessor implements MessagePostProcessor {

    private final TenantProvider tenantProvider;

    public TenantAwareMessagePostProcessor(TenantProvider tenantProvider) {
        this.tenantProvider = tenantProvider;
    }

    @Override
    public Message postProcessMessage(Message message) {

        String tenant = message.getMessageProperties()
                .getHeader("tenant-id");

        // Header quando presente; tenant fixo da instalação como fallback (D6).
        TenantContext.set(tenant != null && !tenant.isBlank()
                ? TenantId.fromString(tenant)
                : tenantProvider.tenantPadrao());

        return message;
    }
}