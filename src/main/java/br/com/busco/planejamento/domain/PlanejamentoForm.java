package br.com.busco.planejamento.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Formulário de edição parcial: só campos presentes (não-nulos) são aplicados.
 * Referências externas viajam como IDs opacos.
 */
@Getter
@RequiredArgsConstructor
public class PlanejamentoForm {

    private final Consumer<PlanejamentoForm> action;

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private LocalTime horaViagem;
    private List<DayOfWeek> diasDaSemana;
    private List<UUID> calendarioIds;
    private UUID motoristaId;
    private UUID rotaId;
    private UUID veiculoId;
    private UUID monitorId;
    private List<Passageiro> passageiros;
    private UUID grupoChecklistInicialId;
    private UUID grupoChecklistFinalId;

    public PlanejamentoForm dataInicio(final LocalDate dataInicio) {
        this.dataInicio = dataInicio;
        return this;
    }

    public PlanejamentoForm dataFim(final LocalDate dataFim) {
        this.dataFim = dataFim;
        return this;
    }

    public PlanejamentoForm horaViagem(final LocalTime horaViagem) {
        this.horaViagem = horaViagem;
        return this;
    }

    public PlanejamentoForm diasDaSemana(final List<DayOfWeek> diasDaSemana) {
        this.diasDaSemana = diasDaSemana;
        return this;
    }

    public PlanejamentoForm calendarioIds(final List<UUID> calendarioIds) {
        this.calendarioIds = calendarioIds;
        return this;
    }

    public PlanejamentoForm motoristaId(final UUID motoristaId) {
        this.motoristaId = motoristaId;
        return this;
    }

    public PlanejamentoForm passageiros(final List<Passageiro> passageiros) {
        this.passageiros = passageiros;
        return this;
    }

    public PlanejamentoForm rotaId(final UUID rotaId) {
        this.rotaId = rotaId;
        return this;
    }

    public PlanejamentoForm veiculoId(final UUID veiculoId) {
        this.veiculoId = veiculoId;
        return this;
    }

    public PlanejamentoForm monitorId(final UUID monitorId) {
        this.monitorId = monitorId;
        return this;
    }

    public PlanejamentoForm grupoChecklistInicialId(final UUID grupoChecklistInicialId) {
        this.grupoChecklistInicialId = grupoChecklistInicialId;
        return this;
    }

    public PlanejamentoForm grupoChecklistFinalId(final UUID grupoChecklistFinalId) {
        this.grupoChecklistFinalId = grupoChecklistFinalId;
        return this;
    }

    public void aplicar() {
        action.accept(this);
    }
}
