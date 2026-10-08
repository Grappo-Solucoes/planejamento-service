package br.com.busco.planejamento.infra.rabbit;

/**
 * Topologia de mensageria do contrato v1 (docs/events/v1/CONTRACT.md §1).
 * Nomes congelados: não alterar sem nova versão do contrato.
 */
public final class RabbitTopologia {

    public static final String COMMANDS_EXCHANGE = "planejamento.exchange";
    public static final String COMMANDS_QUEUE = "planejamento.commands.v1";
    public static final String COMMANDS_DLQ = "planejamento.commands.v1.dlq";

    public static final String EVENTS_EXCHANGE = "planejamento.events.exchange";
    public static final String EVENTS_QUEUE = "busco-api.planejamento.v1";
    public static final String EVENTS_DLQ = "busco-api.planejamento.v1.dlq";

    public static final String DLX = "planejamento.dlx";

    private RabbitTopologia() {
    }
}
