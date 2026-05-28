package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.domain.events.*;
import br.com.busco.planejamento.sk.ddd.AbstractAggregateRoot;
import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static br.com.busco.planejamento.sk.ids.PlanejamentoLoteId.randomId;

@Table
@Entity
@Getter
@EqualsAndHashCode(of = {"periodo", "recorrencia", "rota", "veiculo", "motorista"}, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
public final class PlanejamentoLote  extends AbstractAggregateRoot<PlanejamentoLoteId> {

    @Enumerated(EnumType.STRING)
    private StatusPlanejamentoLote status;
    private final Periodo periodo;
    private final RecorrenciaSemanal recorrencia;

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "rota"))
    private final RotaId rota;

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "veiculo"))
    private final VeiculoId veiculo;

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "motorista"))
    private final MotoristaId motorista;

    @Builder
    private PlanejamentoLote(
            Periodo periodo,
            RecorrenciaSemanal recorrencia,
            RotaId rota,
            VeiculoId veiculo,
            MotoristaId motorista
    ) {
        super(randomId());
        this.status = StatusPlanejamentoLote.RASCUNHO;
        this.periodo = periodo;
        this.recorrencia = recorrencia;
        this.rota = rota;
        this.veiculo = veiculo;
        this.motorista = motorista;

        this.registerEvent(PlanejamentoLoteCriado.of(this));
    }

    public void ativar() {
        if (status != StatusPlanejamentoLote.RASCUNHO) {
            throw new IllegalStateException("Somente rascunhos podem ser ativados");
        }
        this.status = StatusPlanejamentoLote.ATIVO;
        this.registerEvent(PlanejamentoLoteConfirmado.of(this));
    }

    public void suspender() {
        if (status != StatusPlanejamentoLote.ATIVO) {
            throw new IllegalStateException("Somente planejamento ativo pode ser suspenso");
        }
        this.status = StatusPlanejamentoLote.SUSPENSO;
        this.registerEvent(PlanejamentoLoteSuspenso.of(this));

    }

    public void cancelar() {
        this.status = StatusPlanejamentoLote.CANCELADO;
        this.registerEvent(PlanejamentoLoteCancelado.of(this));
    }

    public List<LocalDateTime> gerarDatas() {

        if (status == StatusPlanejamentoLote.CANCELADO) {
            throw new IllegalStateException("Planejamento cancelado não pode gerar datas");
        }

        List<LocalDateTime> datas = new ArrayList<>();

        LocalDateTime dataAtual = periodo.getInicio();

        while (!dataAtual.isAfter(periodo.getFim())) {

            if (recorrencia.ocorreNoDia(dataAtual.getDayOfWeek())) {
                datas.add(dataAtual);
            }

            dataAtual = dataAtual.plusDays(1);
        }

        return datas;
    }


}
