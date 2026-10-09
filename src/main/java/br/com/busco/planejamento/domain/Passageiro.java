package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ddd.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Snapshot do passageiro dentro do planejamento. O cadastro (ativo/inativo,
 * nome, ponto) vive em outro contexto; aqui guardamos só o necessário para
 * planejar, sem FK cruzada. O histórico do planejamento preserva o snapshot
 * mesmo após inativação na origem.
 */
@Getter
@Embeddable
@EqualsAndHashCode(of = "passageiroId")
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Passageiro implements ValueObject {

    @Column(name = "passageiro_id")
    private final UUID passageiroId;

    private final String nome;

    private final String ponto;

    private final boolean ativo;

    public static Passageiro of(UUID passageiroId, String nome, String ponto, boolean ativo) {
        return new Passageiro(passageiroId, nome, ponto, ativo);
    }

    /** Referência opaca (só ID conhecido) — assume ativo até sincronização com a origem. */
    public static Passageiro porId(UUID passageiroId) {
        return new Passageiro(passageiroId, null, null, true);
    }

    public boolean isAtivo() {
        return ativo;
    }
}
