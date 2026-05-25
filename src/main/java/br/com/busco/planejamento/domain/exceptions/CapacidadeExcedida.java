package br.com.busco.planejamento.domain.exceptions;

public final class CapacidadeExcedida extends IllegalStateException {
    public CapacidadeExcedida(String message) {
        super(message);
    }
}
