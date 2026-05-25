package br.com.busco.planejamento.domain.exceptions;

public final class SomenteAgendamentosEmLotePodemSerCancelados extends IllegalStateException {
    public SomenteAgendamentosEmLotePodemSerCancelados() {
        super("Somente agendamentos de lote podem ser cancelados por planejamento");
    }
}
