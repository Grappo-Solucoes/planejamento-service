package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.domain.events.PlanejamentoCancelado;
import br.com.busco.planejamento.domain.events.PlanejamentoEditado;
import br.com.busco.planejamento.domain.events.ViagemPlanejada;
import br.com.busco.planejamento.sk.ddd.AbstractAggregateRoot;
import br.com.busco.planejamento.sk.ids.PlanejamentoId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Agregado de planejamento desacoplado (paridade com
 * {@code br.com.grappo.bus.core.planejamento.domain.Planejamento}).
 *
 * <p>Rota, motorista, monitor, veículo, calendários e checklists são
 * referenciados só por ID (opacos, sem validação nem FK cruzada).
 * Passageiros são VOs próprios (snapshot id + nome + ponto + ativo).
 */
@Table(name = "planejamento")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
public class Planejamento extends AbstractAggregateRoot<PlanejamentoId> {

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDate dataInicio;
    private LocalDate dataFim;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "planejamento_dias_da_semana", joinColumns = @JoinColumn(name = "planejamento_id"))
    @Column(name = "dia_da_semana", nullable = false)
    @Enumerated(EnumType.STRING)
    private List<DayOfWeek> diasDaSemana;

    private LocalTime horaViagem;

    @Column(name = "rota_id")
    private UUID rotaId;

    @Column(name = "motorista_id")
    private UUID motoristaId;

    @Column(name = "veiculo_id")
    private UUID veiculoId;

    @Column(name = "monitor_id")
    private UUID monitorId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "planejamento_passageiro", joinColumns = @JoinColumn(name = "planejamento_id"))
    private List<Passageiro> passageiros = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "planejamento_calendario", joinColumns = @JoinColumn(name = "planejamento_id"))
    @Column(name = "calendario_id", nullable = false)
    private List<UUID> calendarioIds = new ArrayList<>();

    @Column(name = "grupo_checklist_inicial_id")
    private UUID grupoChecklistInicialId;

    @Column(name = "grupo_checklist_final_id")
    private UUID grupoChecklistFinalId;

    private String codigo;

    @Builder
    private Planejamento(LocalDate dataInicio,
                         LocalDate dataFim,
                         List<DayOfWeek> diasDaSemana,
                         LocalTime horaViagem,
                         UUID rotaId,
                         UUID motoristaId,
                         UUID veiculoId,
                         UUID monitorId,
                         List<Passageiro> passageiros,
                         List<UUID> calendarioIds,
                         UUID grupoChecklistInicialId,
                         UUID grupoChecklistFinalId,
                         String codigo) {
        super(PlanejamentoId.randomId());
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.diasDaSemana = diasDaSemana != null ? new ArrayList<>(diasDaSemana) : new ArrayList<>();
        this.horaViagem = horaViagem;
        this.rotaId = rotaId;
        this.motoristaId = motoristaId;
        this.veiculoId = veiculoId;
        this.monitorId = monitorId;
        this.passageiros = passageiros != null ? new ArrayList<>(passageiros) : new ArrayList<>();
        this.calendarioIds = calendarioIds != null ? new ArrayList<>(calendarioIds) : new ArrayList<>();
        this.grupoChecklistInicialId = grupoChecklistInicialId;
        this.grupoChecklistFinalId = grupoChecklistFinalId;
        this.codigo = codigo;
        this.status = Status.EM_ANALISE;
    }

    public void planejar() {
        this.registerEvent(ViagemPlanejada.of(getId()));
        this.status = status.planejar();
    }

    public void iniciar() {
        this.status = status.iniciar();
    }

    public void finalizar() {
        this.status = status.finalizar();
    }

    public void cancelar() {
        this.registerEvent(PlanejamentoCancelado.of(getId()));
        this.status = status.cancelar();
    }

    /**
     * Alunos que devem participar de novas viagens geradas por este planejamento.
     * O relacionamento completo permanece em {@link #passageiros} para preservar o histórico.
     */
    public List<Passageiro> getPassageirosAtivos() {
        return passageiros.stream().filter(Passageiro::isAtivo).toList();
    }

    public PlanejamentoForm update() {
        return new PlanejamentoForm(form -> {
            if (form.getDataInicio() != null) {
                this.dataInicio = form.getDataInicio();
            }
            if (form.getDataFim() != null) {
                this.dataFim = form.getDataFim();
            }
            if (form.getHoraViagem() != null) {
                this.horaViagem = form.getHoraViagem();
            }
            if (form.getDiasDaSemana() != null) {
                this.diasDaSemana = new ArrayList<>(form.getDiasDaSemana());
            }
            if (form.getCalendarioIds() != null) {
                this.calendarioIds = new ArrayList<>(form.getCalendarioIds());
            }
            if (form.getRotaId() != null) {
                this.rotaId = form.getRotaId();
            }
            if (form.getMotoristaId() != null) {
                this.motoristaId = form.getMotoristaId();
            }
            if (form.getVeiculoId() != null) {
                this.veiculoId = form.getVeiculoId();
            }
            if (form.getMonitorId() != null) {
                this.monitorId = form.getMonitorId();
            }
            if (form.getPassageiros() != null) {
                this.passageiros = new ArrayList<>(form.getPassageiros());
            }
            if (form.getGrupoChecklistInicialId() != null) {
                this.grupoChecklistInicialId = form.getGrupoChecklistInicialId();
            }
            if (form.getGrupoChecklistFinalId() != null) {
                this.grupoChecklistFinalId = form.getGrupoChecklistFinalId();
            }
            this.registerEvent(PlanejamentoEditado.of(getId()));
        });
    }

    /** Expõe os eventos registrados (uso em testes de domínio). */
    public List<Object> domainEventsForTest() {
        return new ArrayList<>((Collection<Object>) domainEvents());
    }
}
