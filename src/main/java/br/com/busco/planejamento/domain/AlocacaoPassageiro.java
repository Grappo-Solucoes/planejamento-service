// AlocacaoPassageiro.java - NOVA ENTIDADE
package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ddd.AbstractEntity;
import br.com.busco.planejamento.sk.ids.AlocacaoPassageiroId;
import br.com.busco.planejamento.sk.ids.PassageiroId;
import br.com.busco.planejamento.sk.ids.PontoId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "alocacao_passageiro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlocacaoPassageiro extends AbstractEntity<AlocacaoPassageiroId> {

    @EmbeddedId
    private AlocacaoPassageiroId id;

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "passageiro_id"))
    private PassageiroId passageiroId;

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "ponto_embarque_id"))
    private PontoId pontoEmbarque;

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "ponto_desembarque_id"))
    private PontoId pontoDesembarque;

    @Enumerated(EnumType.STRING)
    private StatusAlocacao status;

    private LocalDateTime confirmadoEm;
    private LocalDateTime canceladoEm;
    private String canceladoPor;

    @Builder
    private AlocacaoPassageiro(
            PassageiroId passageiroId,
            PontoId pontoEmbarque,
            PontoId pontoDesembarque
    ) {
        Objects.requireNonNull(passageiroId, "Passageiro é obrigatório");
        Objects.requireNonNull(pontoEmbarque, "Ponto de embarque é obrigatório");
        Objects.requireNonNull(pontoDesembarque, "Ponto de desembarque é obrigatório");

        if (pontoEmbarque.equals(pontoDesembarque)) {
            throw new IllegalArgumentException("Ponto de embarque e desembarque não podem ser iguais");
        }

        this.id = AlocacaoPassageiroId.randomId();
        this.passageiroId = passageiroId;
        this.pontoEmbarque = pontoEmbarque;
        this.pontoDesembarque = pontoDesembarque;
        this.status = StatusAlocacao.RESERVADA;
    }

    public void confirmar() {
        if (status != StatusAlocacao.RESERVADA) {
            throw new IllegalStateException("Apenas alocações reservadas podem ser confirmadas");
        }
        this.status = StatusAlocacao.CONFIRMADA;
        this.confirmadoEm = LocalDateTime.now();
    }

    public void cancelar(String motivo, String canceladoPor) {
        if (status == StatusAlocacao.REALIZADA) {
            throw new IllegalStateException("Alocação já realizada não pode ser cancelada");
        }
        if (status == StatusAlocacao.CANCELADA) {
            return;
        }
        this.status = StatusAlocacao.CANCELADA;
        this.canceladoEm = LocalDateTime.now();
        this.canceladoPor = canceladoPor;
    }

    public void realizar() {
        if (status != StatusAlocacao.CONFIRMADA) {
            throw new IllegalStateException("Apenas alocações confirmadas podem ser realizadas");
        }
        this.status = StatusAlocacao.REALIZADA;
    }

}