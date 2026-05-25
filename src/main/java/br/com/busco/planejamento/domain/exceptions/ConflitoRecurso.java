package br.com.busco.planejamento.domain.exceptions;

public final class ConflitoRecurso extends IllegalStateException {
    public ConflitoRecurso(String message) {
        super(message);
    }
}
