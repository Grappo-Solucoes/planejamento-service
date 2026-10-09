package br.com.busco.planejamento.infra.job;

import br.com.busco.planejamento.app.AgendamentoService;
import br.com.busco.planejamento.app.cmd.ColocarAgendamentoEmConflito;
import br.com.busco.planejamento.app.cmd.ConfirmarAgendamento;
import br.com.busco.planejamento.app.cmd.RevalidarAgendamento;
import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

//TODO: Implementar Saga
@Slf4j
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
            // Revalidação parte de CONFIRMADO; o fallback de conflito exigiria
            // EM_ANALISE e seria sempre inválido. Aqui não há fallback: uma falha
            // é apenas registrada e o item segue para os demais.
            emTransacaoPropria(
                    () -> service.handle(RevalidarAgendamento.builder().id(agendamento).build()),
                    null);
        }
    }

    /**
     * Isola cada item do lote em transação própria: uma rejeição no comando do
     * item reverte apenas essa transação, e o fallback de conflito commita em
     * transação nova — a transação condenada (rollback-only propagado pelo
     * proxy do serviço) não é reaproveitada, evitando que um único item com
     * falha reverta o lote inteiro com {@code UnexpectedRollbackException}.
     *
     * <p>O fallback também é protegido: se ele mesmo falhar (ou for
     * {@code null}, quando o fluxo não tem fallback válido, como na
     * revalidação), a falha é registrada e o item é ignorado — os itens
     * restantes continuam sendo processados.</p>
     */
    private void emTransacaoPropria(Runnable comando, Runnable fallbackConflito) {
        try {
            transactionTemplate.executeWithoutResult(status -> comando.run());
        } catch (Exception e) {
            log.warn("Item do auto-processamento falhou; {}", fallbackConflito == null
                    ? "fluxo sem fallback de conflito, apenas registrando"
                    : "aplicando fallback de conflito", e);
            if (fallbackConflito == null) {
                return;
            }
            try {
                transactionTemplate.executeWithoutResult(status -> fallbackConflito.run());
            } catch (Exception falhaFallback) {
                log.error("Fallback de conflito do auto-processamento também falhou; "
                        + "o item é ignorado e os demais seguem", falhaFallback);
            }
        }
    }
}
