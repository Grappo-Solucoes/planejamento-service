package br.com.busco.planejamento.domain.exceptions;

public final class NaoPossivelCancelarAgendamentosRealizados extends IllegalStateException {
    public NaoPossivelCancelarAgendamentosRealizados() {
        super("Não é possível cancelar agendamento já realizado");
    }
}
