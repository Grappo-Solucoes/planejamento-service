package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.CancelarPlanejamento;
import br.com.busco.planejamento.app.cmd.DuplicarPlanejamento;
import br.com.busco.planejamento.app.cmd.EditarPlanejamento;
import br.com.busco.planejamento.app.cmd.PlanejarViagem;
import br.com.busco.planejamento.app.cmd.ValidarPlanejamento;
import br.com.busco.planejamento.domain.Passageiro;
import br.com.busco.planejamento.domain.Planejamento;
import br.com.busco.planejamento.domain.PlanejamentoRepository;
import br.com.busco.planejamento.domain.Status;
import br.com.busco.planejamento.sk.ids.PlanejamentoId;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.LockModeType.PESSIMISTIC_READ;
import static java.lang.String.format;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Serviço de escritas do planejamento (paridade com
 * {@code br.com.grappo.bus.core.planejamento.app.PlanejamentoService}).
 *
 * <p>Referências externas (rota, motorista, veículo, monitor, calendários,
 * checklists, alunos) são IDs opacos: este contexto não lê nem escreve em
 * tabelas de outros contextos. Efeitos sobre viagens são publicados como
 * eventos de domínio, nunca como escrita direta.
 */
@Log
@Service
@AllArgsConstructor
@Transactional(propagation = REQUIRES_NEW)
public class PlanejamentoService {

    private final PlanejamentoRepository repository;

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public UUID handle(@NonNull @Valid PlanejarViagem cmd) {
        Planejamento planejamento = Planejamento.builder()
                .dataInicio(cmd.getDataInicio())
                .dataFim(cmd.getDataFim())
                .diasDaSemana(cmd.getDiasSemana())
                .horaViagem(cmd.getHoraViagem())
                .rotaId(cmd.getRota())
                .motoristaId(cmd.getMotorista())
                .veiculoId(cmd.getVeiculo())
                .monitorId(cmd.getMonitor())
                .passageiros(paraPassageiros(cmd.getAlunos()))
                .calendarioIds(cmd.getCalendarios())
                .grupoChecklistInicialId(cmd.getGrupoChecklistInicial())
                .grupoChecklistFinalId(cmd.getGrupoChecklistFinal())
                .codigo(gerarCodigo())
                .build();

        return UUID.fromString(repository.save(planejamento).getId().toUUID());
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public UUID handle(@NonNull @Valid EditarPlanejamento cmd) {
        Planejamento planejamento = buscarPorId(PlanejamentoId.fromString(cmd.getId().toString()));

        planejamento.update()
                .dataInicio(cmd.getDataInicio())
                .dataFim(cmd.getDataFim())
                .horaViagem(cmd.getHoraViagem())
                .diasDaSemana(cmd.getDiasSemana())
                .calendarioIds(cmd.getCalendarios())
                .rotaId(cmd.getRota())
                .motoristaId(cmd.getMotorista())
                .veiculoId(cmd.getVeiculo())
                .monitorId(cmd.getMonitor())
                .passageiros(cmd.getAlunos() != null ? paraPassageiros(cmd.getAlunos()) : null)
                .grupoChecklistInicialId(cmd.getGrupoChecklistInicial())
                .grupoChecklistFinalId(cmd.getGrupoChecklistFinal())
                .aplicar();

        return UUID.fromString(repository.save(planejamento).getId().toUUID());
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public UUID handle(@NonNull @Valid ValidarPlanejamento cmd) {
        Planejamento planejamento = buscarPorId(PlanejamentoId.fromString(cmd.getId().toString()));
        if (planejamento.getStatus() != Status.EM_ANALISE) {
            throw new IllegalStateException(
                    format("Planejamento %s não está em análise (status atual: %s).",
                            cmd.getId(), planejamento.getStatus()));
        }
        planejamento.planejar();
        return UUID.fromString(repository.save(planejamento).getId().toUUID());
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public UUID handle(@NonNull @Valid CancelarPlanejamento cmd) {
        Planejamento planejamento = buscarPorId(PlanejamentoId.fromString(cmd.getId().toString()));
        if (planejamento.getStatus() != Status.EM_ANALISE
                && planejamento.getStatus() != Status.PLANEJADA) {
            throw new IllegalStateException(
                    format("Planejamento %s não pode ser cancelado (status atual: %s).",
                            cmd.getId(), planejamento.getStatus()));
        }
        planejamento.cancelar();
        return UUID.fromString(repository.save(planejamento).getId().toUUID());
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public UUID handle(@NonNull @Valid DuplicarPlanejamento cmd) {
        Planejamento origem = buscarPorId(PlanejamentoId.fromString(cmd.getId().toString()));

        Planejamento copia = Planejamento.builder()
                .dataInicio(origem.getDataInicio())
                .dataFim(origem.getDataFim())
                .diasDaSemana(new ArrayList<>(origem.getDiasDaSemana()))
                .horaViagem(origem.getHoraViagem())
                .rotaId(origem.getRotaId())
                .motoristaId(origem.getMotoristaId())
                .veiculoId(origem.getVeiculoId())
                .monitorId(origem.getMonitorId())
                .passageiros(new ArrayList<>(origem.getPassageirosAtivos()))
                .calendarioIds(new ArrayList<>(origem.getCalendarioIds()))
                .grupoChecklistInicialId(origem.getGrupoChecklistInicialId())
                .grupoChecklistFinalId(origem.getGrupoChecklistFinalId())
                .codigo(gerarCodigo())
                .build();

        return UUID.fromString(repository.save(copia).getId().toUUID());
    }

    @NonNull
    @Transactional(readOnly = true)
    public Planejamento buscarPorId(@NonNull PlanejamentoId id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        format("Not found any Business with code %s.", id.toUUID())));
    }

    private List<Passageiro> paraPassageiros(List<UUID> alunos) {
        if (alunos == null) {
            return new ArrayList<>();
        }
        return alunos.stream().map(Passageiro::porId).toList();
    }

    private String gerarCodigo() {
        return UUID.randomUUID().toString();
    }
}
