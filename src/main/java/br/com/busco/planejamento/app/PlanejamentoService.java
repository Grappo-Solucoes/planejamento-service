package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.*;
import br.com.busco.planejamento.domain.*;
import br.com.busco.planejamento.domain.policy.PoliticaDeCapacidade;
import br.com.busco.planejamento.domain.policy.PoliticaDeConflito;
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
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@AllArgsConstructor

@Log
@Service
@Validated
@Transactional(propagation = REQUIRES_NEW)
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
                .periodo(periodo)
                .recorrencia(recorrenciaSemanal)
                .rota(cmd.getRota())
                .motorista(cmd.getMotorista())
                .veiculo(cmd.getVeiculo())
                .build();
        return repository.save(planejamento).getId();
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public PlanejamentoLoteId handle(ConfirmarPlanejamentoLote cmd) {
        PlanejamentoLote planejamentoLote = buscarPorId(cmd.getId());
        planejamentoLote.ativar();

        expansaoPlanejamentoDomainService.expandir(planejamentoLote, LocalDateTime.now());

        return repository.save(planejamentoLote).getId();
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public PlanejamentoLoteId handle(CancelarPlanejamentoLote cmd) {
        PlanejamentoLote planejamentoLote = buscarPorId(cmd.getId());
        List<AgendamentoOperacionalId> agendamentosFuturos =
                agendamentoRepository.buscarIdFuturosPorPlanejamento(
                        planejamentoLote.getId(),
                        LocalDateTime.now()
                );

        for (AgendamentoOperacionalId id : agendamentosFuturos) {
            agendamentoService.handle(CancelarAgendamentoPorLote.builder().id(id).build());
        }


        planejamentoLote.cancelar();

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
