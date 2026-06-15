package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.domain.events.*;
import br.com.busco.planejamento.domain.exceptions.CapacidadeExcedida;
import br.com.busco.planejamento.domain.exceptions.ConflitoRecurso;
import br.com.busco.planejamento.domain.exceptions.NaoPossivelCancelarAgendamentosRealizados;
import br.com.busco.planejamento.domain.exceptions.SomenteAgendamentosEmLotePodemSerCancelados;
import br.com.busco.planejamento.sk.ddd.AbstractAggregateRoot;
import br.com.busco.planejamento.sk.ids.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static br.com.busco.planejamento.domain.StatusAgendamento.*;
import static br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId.randomId;

@Table
@Entity
@Getter
@EqualsAndHashCode(of = {"rota", "veiculo", "motorista", "data"}, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
public class AgendamentoOperacional extends AbstractAggregateRoot<AgendamentoOperacionalId> {

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "rota_id"))
    private final RotaId rota;

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "veiculo_id"))
    private VeiculoId veiculo;

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "motorista_id"))
    private final MotoristaId motorista;
    private final LocalDateTime data;

    @Embedded
    private final OrigemAgendamento origem;

    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;

    @Embedded
    private CapacidadeVeiculo capacidadeVeiculo;

    @OneToMany(mappedBy = "agendamento", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<AlocacaoPassageiro> alocacoes = new HashSet<>();

    @Builder
    private AgendamentoOperacional(RotaId rota, VeiculoId veiculo, MotoristaId motorista, OrigemAgendamento origem, LocalDateTime data, CapacidadeVeiculo capacidadeVeiculo) {
        super(randomId());
        this.rota = Objects.requireNonNull(rota, "Rota é obrigatória");
        this.veiculo = Objects.requireNonNull(veiculo, "Veículo é obrigatório");
        this.motorista = Objects.requireNonNull(motorista, "Motorista é obrigatório");
        this.data = Objects.requireNonNull(data, "Data é obrigatória");
        this.origem = Objects.requireNonNull(origem, "Origem é obrigatória");
        this.status = EM_ANALISE;
        this.capacidadeVeiculo = capacidadeVeiculo != null ? capacidadeVeiculo : CapacidadeVeiculo.of(44, 0, 0, 0);

        this.registerEvent(AgendamentoCriado.of(this));
    }

    public void alocarPassageiro(
            PassageiroId passageiroId,
            PontoId pontoEmbarque,
            PontoId pontoDesembarque,
            TipoPassageiro tipo
    ) {
        if (status != EM_ANALISE && status != CONFIRMADO) {
            throw new IllegalStateException(
                    String.format("Não é possível alocar passageiro em agendamento com status %s", status)
            );
        }

        if (passageiroJaAlocado(passageiroId)) {
            throw new IllegalStateException("Passageiro já está alocado neste agendamento");
        }

        int quantidadeAtual = alocacoes.size();

        if (!capacidadeVeiculo.podeAlocar(tipo, quantidadeAtual)) {
            throw new CapacidadeExcedida(
                    String.format("Capacidade excedida para tipo %s. Atual: %d",
                            tipo, quantidadeAtual)
            );
        }

        AlocacaoPassageiro alocacao = AlocacaoPassageiro.builder()
                .passageiroId(passageiroId)
                .pontoEmbarque(pontoEmbarque)
                .pontoDesembarque(pontoDesembarque)
                .build();
        alocacao.vincularAgendamento(this);

        alocacoes.add(alocacao);

        registerEvent(PassageiroAlocado.of(this, alocacao));
    }

    public void removerPassageiro(PassageiroId passageiroId, String motivo, String removidoPor) {
        if (status == REALIZADO) {
            throw new IllegalStateException("Não é possível remover passageiro de viagem já realizada");
        }

        AlocacaoPassageiro alocacao = buscarAlocacao(passageiroId);
        alocacao.cancelar(motivo, removidoPor);
        alocacoes.remove(alocacao);

        registerEvent(PassageiroRemovido.of(this, passageiroId, motivo));
    }

    public void trocarVeiculo(VeiculoId novoVeiculo, CapacidadeVeiculo novaCapacidade) {
        if (status == REALIZADO) {
            throw new IllegalStateException("Não é possível trocar veículo de viagem já realizada");
        }

        if (getQuantidadePassageiros() > novaCapacidade.getCapacidadeTotal()) {
            throw new CapacidadeExcedida(
                    String.format("Novo veículo tem capacidade %d, mas existem %d passageiros alocados",
                            novaCapacidade.getCapacidadeTotal(), getQuantidadePassageiros())
            );
        }

        VeiculoId veiculoAntigo = this.veiculo;
        this.veiculo = novoVeiculo;
        this.capacidadeVeiculo = novaCapacidade;

        registerEvent(VeiculoSubstituido.of(this, veiculoAntigo, novoVeiculo));
    }


    public void confirmar() {
        if (status != EM_ANALISE) {
            throw new IllegalStateException("Somente agendamentos em análise podem ser confirmados");
        }

        if (alocacoes.isEmpty()) {
            throw new IllegalStateException("Não é possível confirmar agendamento sem passageiros");
        }

        this.status = CONFIRMADO;
        alocacoes.forEach(AlocacaoPassageiro::confirmar);

        this.registerEvent(AgendamentoConfirmado.of(this));
    }

    private boolean passageiroJaAlocado(PassageiroId passageiroId) {
        return alocacoes.stream()
                .anyMatch(a -> a.getPassageiroId().equals(passageiroId));
    }

    private AlocacaoPassageiro buscarAlocacao(PassageiroId passageiroId) {
        return alocacoes.stream()
                .filter(a -> a.getPassageiroId().equals(passageiroId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Passageiro não alocado neste agendamento"));
    }

    public boolean temConflitoRecurso(AgendamentoOperacional outro) {
        if (!this.data.equals(outro.data)) return false;

        boolean mesmoVeiculo = this.veiculo.equals(outro.veiculo);
        boolean mesmoMotorista = this.motorista.equals(outro.motorista);

        return mesmoVeiculo || mesmoMotorista;
    }

    public int getQuantidadePassageiros() {
        return alocacoes.size();
    }

    public Set<PassageiroId> getPassageirosAlocados() {
        return alocacoes.stream()
                .map(AlocacaoPassageiro::getPassageiroId)
                .collect(Collectors.toSet());
    }



    public void cancelar() {
        if (status == CANCELADO) {
            throw new IllegalStateException("Agendamento já cancelado");
        }

        this.status = CANCELADO;

        alocacoes.forEach(a -> a.cancelar("Agendamento cancelado", "SISTEMA"));

        this.registerEvent(AgendamentoCancelado.of(this));
    }

    public void cancelarPorPlanejamento() {

        if (origem.ehAvulso()) {
            throw new SomenteAgendamentosEmLotePodemSerCancelados();
        }

        if (status == REALIZADO) {
            throw new NaoPossivelCancelarAgendamentosRealizados();
        }

        if (status == StatusAgendamento.CANCELADO) {
            return;
        }

        this.status = StatusAgendamento.CANCELADO;
        alocacoes.forEach(a -> a.cancelar("Cancelado por planejamento", "SISTEMA"));

        registerEvent(AgendamentoCanceladoPorPlanejamento.of(this));
    }

    public void revalidar() {
        if (status != CONFIRMADO) {
            throw new IllegalStateException("Somente confirmados podem ser revalidados");
        }

        this.registerEvent(AgendamentoRevalidado.of(this));
    }

    public void colocarEmConflito() {
        if (status != EM_ANALISE) {
            throw new IllegalStateException("Somente agendamentos em análise podem ser marcados com conflito");
        }

        this.status = StatusAgendamento.CONFLITO;
        this.registerEvent(AgendamentoEmConflito.of(this));
    }

    public void realizar() {
        if (status != CONFIRMADO) {
            throw new IllegalStateException("Somente confirmados podem ser realizados");
        }
        this.status = REALIZADO;
        alocacoes.forEach(AlocacaoPassageiro::realizar);

        this.registerEvent(AgendamentoRealizado.of(this));
    }

}
