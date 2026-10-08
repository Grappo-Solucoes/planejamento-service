package br.com.busco.planejamento.sk.ddd;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TenantAwareMessagePostProcessor - Testes Unitários")
class TenantAwareMessagePostProcessorTest {

    private static final String TENANT_FIXO = "00000000-0000-0000-0000-000000000001";

    private final TenantAwareMessagePostProcessor postProcessor =
            new TenantAwareMessagePostProcessor(new TenantProvider(TENANT_FIXO));

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Usa o header tenant-id quando presente")
    void deveUsarHeaderQuandoPresente() {
        String tenant = UUID.randomUUID().toString();
        MessageProperties props = new MessageProperties();
        props.setHeader("tenant-id", tenant);

        postProcessor.postProcessMessage(new Message("{}".getBytes(StandardCharsets.UTF_8), props));

        assertThat(TenantContext.get()).isEqualTo(TenantId.fromString(tenant));
    }

    @Test
    @DisplayName("Usa o tenant fixo quando o header está ausente")
    void deveUsarTenantFixoQuandoHeaderAusente() {
        postProcessor.postProcessMessage(new Message(new byte[0], new MessageProperties()));

        assertThat(TenantContext.get()).isEqualTo(TenantId.fromString(TENANT_FIXO));
    }
}
