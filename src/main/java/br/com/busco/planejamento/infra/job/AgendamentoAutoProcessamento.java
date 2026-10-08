package br.com.busco.planejamento.infra.job;

import br.com.busco.planejamento.app.AgendamentoService;
import br.com.busco.planejamento.app.cmd.ColocarAgendamentoEmConflito;
import br.com.busco.planejamento.app.cmd.ConfirmarAgendamento;
import br.com.busco.planejamento.app.cmd.RevalidarAgendamento;
import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

//TODO: Implementar Saga
@Component
@Profile("!test")
@ConditionalOnProperty(name = "planejamento.jobs.auto-processamento.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AgendamentoAutoProcessamento {
    private final AgendamentoService service;
    private final AgendamentoRepository repository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "0 0 */6 * * *") // a cada 6 horas
    public void executar() {
        processarConfirmacoes();
        processarRevalidacoes();
    }

    public void processarConfirmacoes() {
        LocalDateTime limite = LocalDateTime.now().plusDays(7);
        List<AgendamentoOperacionalId> agendamentosConfirmar = repository.buscarIdEmAnaliseAte(limite);
        for (AgendamentoOperacionalId agendamento : agendamentosConfirmar) {
            emTransacaoPropria(
                    () -> service.handle(ConfirmarAgendamento.builder().id(agendamento).build()),
                    () -> service.handle(ColocarAgendamentoEmConflito.builder().id(agendamento).build()));
        }
    }

    public void processarRevalidacoes() {
        LocalDateTime limite = LocalDateTime.now().plusDays(2);
        List<AgendamentoOperacionalId> agendamentosConfirmar = repository.buscarIdConfirmadosAte(limite);
        for (AgendamentoOperacionalId agendamento : agendamentosConfirmar) {
            emTransacaoPropria(
                    () -> service.handle(RevalidarAgendamento.builder().id(agendamento).build()),
                    () -> service.handle(ColocarAgendamentoEmConflito.builder().id(agendamento).build()));
        }
    }

    /**
     * Isola cada item do lote em transação própria: uma rejeição no comando do
     * item reverte apenas essa transação, e o fallback de conflito commita em
     * transação nova — a transação condenada (rollback-only propagado pelo
     * proxy do serviço) não é reaproveitada, evitando que um único item com
     * falha reverta o lote inteiro com {@code UnexpectedRollbackException}.
     */
    private void emTransacaoPropria(Runnable comando, Runnable fallbackConflito) {
        try {
            transactionTemplate.executeWithoutResult(status -> comando.run());
        } catch (Exception e) {
            transactionTemplate.executeWithoutResult(status -> fallbackConflito.run());
        }
    }
}
