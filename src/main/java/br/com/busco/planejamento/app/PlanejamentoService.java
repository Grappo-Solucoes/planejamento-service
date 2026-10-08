package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.*;
import br.com.busco.planejamento.domain.*;
import br.com.busco.planejamento.domain.services.ExpansaoPlanejamentoDomainService;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static jakarta.persistence.LockModeType.PESSIMISTIC_READ;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@AllArgsConstructor

@Log
@Service
@Validated
// Propagação REQUIRED: no caminho de comando o serviço participa da transação
// do ProcessamentoComandoService, garantindo estado + outbox + marcador de
// inbox atômicos (CONTRACT §6).
@Transactional
public class PlanejamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final PlanejamentoLoteRepository repository;
    private final ExpansaoPlanejamentoDomainService expansaoPlanejamentoDomainService;
    private final AgendamentoService agendamentoService;

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public PlanejamentoLoteId handle(CriarPlanejamentoLote cmd) {
        Periodo periodo = Periodo.of(cmd.getDataInicio(), cmd.getDataFim());
        RecorrenciaSemanal recorrenciaSemanal = RecorrenciaSemanal.of(cmd.getDiasDaSemana());

        PlanejamentoLote planejamento = PlanejamentoLote.builder()
                .id(cmd.getLoteId())
                .periodo(periodo)
                .recorrencia(recorrenciaSemanal)
                .rota(cmd.getRota())
                .motorista(cmd.getMotorista())
                .veiculo(cmd.getVeiculo())
                .build();
        return repository.save(planejamento).getId();
    }

    /**
     * Confirma o lote e expande a agenda em um único ponto do fluxo (o
     * listener assíncrono de dupla expansão foi removido). O evento
     * {@code lote.confirmado} carrega a contagem de agendamentos gerados
     * nesta confirmação.
     */
    @NonNull
    @Lock(PESSIMISTIC_READ)
    public PlanejamentoLoteId handle(ConfirmarPlanejamentoLote cmd) {
        PlanejamentoLote planejamentoLote = buscarPorId(cmd.getId());
        planejamentoLote.ativar();

        ExpansaoPlanejamentoDomainService.ExpansaoResultado resultado =
                expansaoPlanejamentoDomainService.expandir(planejamentoLote, LocalDateTime.now());

        planejamentoLote.registrarConfirmacao(resultado.agendamentosCriados().size());

        return repository.save(planejamentoLote).getId();
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public PlanejamentoLoteId handle(CancelarPlanejamentoLote cmd) {
        PlanejamentoLote planejamentoLote = buscarPorId(cmd.getId());
        List<AgendamentoOperacionalId> agendamentosFuturos =
                agendamentoRepository.buscarIdFuturosNaoRealizadosPorPlanejamento(
                        planejamentoLote.getId(),
                        LocalDateTime.now()
                );

        for (AgendamentoOperacionalId id : agendamentosFuturos) {
            agendamentoService.handle(CancelarAgendamentoPorLote.builder().id(id).build());
        }

        planejamentoLote.cancelar(agendamentosFuturos.size());

        return repository.save(planejamentoLote).getId();
    }

    /**
     * Edição dos campos de agenda do lote (CONTRACT §5): aplica os novos
     * campos, cancela os agendamentos futuros não executados (status diferente
     * de REALIZADO) e re-expande a agenda quando o lote está ATIVO.
     */
    @NonNull
    @Lock(PESSIMISTIC_READ)
    public PlanejamentoLoteId handle(EditarPlanejamentoLote cmd) {
        PlanejamentoLote planejamentoLote = buscarPorId(cmd.getId());

        Periodo periodo = Periodo.of(cmd.getDataInicio(), cmd.getDataFim());
        RecorrenciaSemanal recorrenciaSemanal = RecorrenciaSemanal.of(cmd.getDiasDaSemana());

        boolean estavaAtivo = planejamentoLote.getStatus() == StatusPlanejamentoLote.ATIVO;

        planejamentoLote.editar(periodo, recorrenciaSemanal, cmd.getRota(), cmd.getVeiculo(), cmd.getMotorista());

        if (estavaAtivo) {
            List<AgendamentoOperacionalId> agendamentosFuturos =
                    agendamentoRepository.buscarIdFuturosNaoRealizadosPorPlanejamento(
                            planejamentoLote.getId(),
                            LocalDateTime.now()
                    );

            for (AgendamentoOperacionalId id : agendamentosFuturos) {
                agendamentoService.handle(CancelarAgendamentoPorEdicao.builder().id(id).build());
            }

            expansaoPlanejamentoDomainService.expandir(planejamentoLote, LocalDateTime.now());
        }

        return repository.save(planejamentoLote).getId();
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public PlanejamentoLoteId handle(SuspenderPlanejamentoLote cmd) {
        PlanejamentoLote planejamentoLote = buscarPorId(cmd.getId());
        planejamentoLote.suspender();

        return repository.save(planejamentoLote).getId();
    }

    @NonNull
    @Transactional(readOnly = true)
    public PlanejamentoLote buscarPorId(@NonNull PlanejamentoLoteId id) {
        return repository.findById(requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException(format("Not found any Account with code %s.", id.toUUID())));
    }
}
