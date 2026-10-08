package br.com.busco.planejamento.infra.rabbit;

import br.com.busco.planejamento.app.ProcessamentoComandoService;
import br.com.busco.planejamento.infra.events.EnvelopeEvento;
import br.com.busco.planejamento.infra.events.EventoJson;
import br.com.busco.planejamento.infra.events.MensagemNaoProcessavelException;
import br.com.busco.planejamento.sk.ddd.TenantContext;
import br.com.busco.planejamento.sk.ddd.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Consome os comandos do busco-api na fila {@code planejamento.commands.v1}
 * (CONTRACT §1). O corpo é JSON puro (String); o tipo é resolvido pelo header
 * {@code event-type}, nunca por {@code __TypeId__}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanejamentoCommandsListener {

    private final EventoJson eventoJson;
    private final ProcessamentoComandoService processamentoComandoService;

    @RabbitListener(queues = RabbitTopologia.COMMANDS_QUEUE)
    public void onComando(Message mensagem) {
        try {
            EnvelopeEvento envelope = lerEnvelope(mensagem);

            if (envelope.tenantId() != null && !envelope.tenantId().isBlank()) {
                TenantContext.set(TenantId.fromString(envelope.tenantId()));
            }

            processamentoComandoService.processar(envelope);
        } finally {
            TenantContext.clear();
        }
    }

    private EnvelopeEvento lerEnvelope(Message mensagem) {
        String json = new String(mensagem.getBody(), StandardCharsets.UTF_8);
        try {
            return eventoJson.deJson(json, EnvelopeEvento.class);
        } catch (RuntimeException e) {
            throw new MensagemNaoProcessavelException("Envelope ilegível: " + e.getMessage(), e);
        }
    }
}
