package br.com.busco.planejamento.infra.events;

import java.util.Set;

/**
 * Catálogo de eventos v1 (CONTRACT §3–§5). Nomes congelados: não alterar sem
 * nova versão do contrato.
 */
public final class EventosV1 {

    public static final String VERSAO = "1.0";

    // Comandos consumidos pelo serviço
    public static final String LOTE_CRIAR_SOLICITADO = "planejamento.lote.criar.solicitado";
    public static final String LOTE_EDITAR_SOLICITADO = "planejamento.lote.editar.solicitado";
    public static final String LOTE_CONFIRMAR_SOLICITADO = "planejamento.lote.confirmar.solicitado";
    public static final String LOTE_CANCELAR_SOLICITADO = "planejamento.lote.cancelar.solicitado";

    // Resultado de falha (publicado pelo serviço)
    public static final String LOTE_FALHA = "planejamento.lote.falha";

    /**
     * Comandos do catálogo v1 consumidos pelo serviço. A validação do
     * consumidor usa somente este conjunto: um eventType de resultado
     * publicado por engano na fila de comandos é rejeitado para a DLQ.
     */
    public static final Set<String> COMANDOS = Set.of(
            LOTE_CRIAR_SOLICITADO,
            LOTE_EDITAR_SOLICITADO,
            LOTE_CONFIRMAR_SOLICITADO,
            LOTE_CANCELAR_SOLICITADO
    );

    /**
     * Todos os tipos do catálogo v1 (comandos e eventos de resultado), para
     * referência do contrato (schemas/testes). A validação do consumidor de
     * comandos usa {@link #COMANDOS}.
     */
    public static final Set<String> CATALOGO = Set.of(
            LOTE_CRIAR_SOLICITADO,
            LOTE_EDITAR_SOLICITADO,
            LOTE_CONFIRMAR_SOLICITADO,
            LOTE_CANCELAR_SOLICITADO,
            DomainEventToIntegrationEventMapper.TIPO_LOTE_CRIADO,
            DomainEventToIntegrationEventMapper.TIPO_LOTE_EDITADO,
            DomainEventToIntegrationEventMapper.TIPO_LOTE_CONFIRMADO,
            DomainEventToIntegrationEventMapper.TIPO_LOTE_CANCELADO,
            LOTE_FALHA,
            DomainEventToIntegrationEventMapper.TIPO_AGENDAMENTO_CRIADO,
            DomainEventToIntegrationEventMapper.TIPO_AGENDAMENTO_CANCELADO
    );

    private EventosV1() {
    }
}
