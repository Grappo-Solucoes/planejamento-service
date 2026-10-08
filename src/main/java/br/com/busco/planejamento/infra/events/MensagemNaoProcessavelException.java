package br.com.busco.planejamento.infra.events;

/**
 * Mensagem que não pode ser processada (envelope malformado, versão não
 * suportada ou {@code eventType} fora do catálogo v1). O container reprocessa
 * conforme a política de retry e, ao esgotar, rejeita para a DLQ sem efeito
 * colateral (CONTRACT §2/§3).
 */
public class MensagemNaoProcessavelException extends RuntimeException {

    public MensagemNaoProcessavelException(String message) {
        super(message);
    }

    public MensagemNaoProcessavelException(String message, Throwable cause) {
        super(message, cause);
    }
}
