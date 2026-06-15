package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.*;
import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.domain.CapacidadeVeiculo;
import br.com.busco.planejamento.domain.OrigemAgendamento;
import br.com.busco.planejamento.domain.policy.PoliticaDeCapacidade;
import br.com.busco.planejamento.domain.policy.PoliticaDeConflito;
import br.com.busco.planejamento.domain.policy.PoliticaDeConflitoTemporal;
import br.com.busco.planejamento.sk.ids.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static jakarta.persistence.LockModeType.PESSIMISTIC_READ;
import static java.util.Objects.requireNonNull;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@AllArgsConstructor

@Log
@Service
@Validated
@Transactional(propagation = REQUIRES_NEW)
public class AgendamentoService {

    private final AgendamentoRepository repository;
    private final PoliticaDeCapacidade politicaDeCapacidade;
    private final PoliticaDeConflito politicaDeConflito;
    private final PoliticaDeConflitoTemporal politicaDeConflitoTemporal;

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public AgendamentoOperacionalId handle(CriarAgendamento cmd) {
        requireNonNull(cmd.getRota(), "Rota é obrigatória");
        requireNonNull(cmd.getVeiculoPadrao(), "Veículo é obrigatório");
        requireNonNull(cmd.getMotoristaPadrao(), "Motorista é obrigatório");
        requireNonNull(cmd.getData(), "Data é obrigatória");

        AgendamentoOperacional agendamento = AgendamentoOperacional.builder()
                .rota(cmd.getRota())
                .motorista(cmd.getMotoristaPadrao())
                .veiculo(cmd.getVeiculoPadrao())
                .data(cmd.getData())
                .origem(OrigemAgendamento.avulso())
                .capacidadeVeiculo(CapacidadeVeiculo.of(44, 0, 0, 0))
                .build();
        AgendamentoOperacional salvo = repository.save(agendamento);

        if (cmd.getPassageiros() != null) {
            cmd.getPassageiros().forEach(passageiro ->
                    alocarPassageiro(salvo, passageiro)
            );
        }

        return repository.save(salvo).getId();
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public AgendamentoOperacionalId handle(AlocarPassageiro cmd) {
        AgendamentoOperacional agendamento = repository.findById(cmd.getAgendamentoId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrada"));

        alocarPassageiro(agendamento, cmd);

        return repository.save(agendamento).getId();
    }

    private void alocarPassageiro(AgendamentoOperacional agendamento, AlocarPassageiro cmd) {
        requireNonNull(cmd.getPassageiroId(), "Passageiro é obrigatório");
        requireNonNull(cmd.getTipo(), "Tipo do passageiro é obrigatório");
        requireNonNull(cmd.getPontoEmbarque(), "Ponto de embarque é obrigatório");
        requireNonNull(cmd.getPontoDesembarque(), "Ponto de desembarque é obrigatório");

        agendamento.alocarPassageiro(
                cmd.getPassageiroId(),
                cmd.getPontoEmbarque(),
                cmd.getPontoDesembarque(),
                cmd.getTipo()
        );
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public AgendamentoOperacionalId handle(ConfirmarAgendamento cmd) {
        AgendamentoOperacional agendamento = repository.findById(cmd.getId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrada"));

        politicaDeConflitoTemporal.validar(agendamento);

        politicaDeCapacidade.validar(agendamento);
        politicaDeConflito.validar(agendamento);

        agendamento.confirmar();

        return repository.save(agendamento).getId();
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public AgendamentoOperacionalId handle(RevalidarAgendamento cmd) {
        AgendamentoOperacional agendamento = repository.findById(cmd.getId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrada"));

        politicaDeCapacidade.validar(agendamento);
        politicaDeConflito.validar(agendamento);
        agendamento.revalidar();

        return repository.save(agendamento).getId();
    }


    @NonNull
    @Lock(PESSIMISTIC_READ)
    public AgendamentoOperacionalId handle(CancelarAgendamento cmd) {
        AgendamentoOperacional agendamento = repository.findById(cmd.getId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrada"));
        agendamento.cancelar();

        return repository.save(agendamento).getId();
    }

    @NonNull
    @Lock(PESSIMISTIC_READ)
    public AgendamentoOperacionalId handle(CancelarAgendamentoPorLote cmd) {
        AgendamentoOperacional agendamento = repository.findById(cmd.getId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrada"));
        agendamento.cancelarPorPlanejamento();

        return repository.save(agendamento).getId();
    }


    @NonNull
    public AgendamentoOperacionalId handle(ColocarAgendamentoEmConflito cmd) {
        AgendamentoOperacional agendamento = repository.findById(cmd.getId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrada"));
        agendamento.colocarEmConflito();

        return repository.save(agendamento).getId();
    }


}
