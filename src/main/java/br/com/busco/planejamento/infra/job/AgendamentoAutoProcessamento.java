package br.com.busco.planejamento.infra.job;


import br.com.busco.planejamento.app.AgendamentoService;
import br.com.busco.planejamento.app.cmd.ColocarAgendamentoEmConflito;
import br.com.busco.planejamento.app.cmd.ConfirmarAgendamento;
import br.com.busco.planejamento.app.cmd.RevalidarAgendamento;
import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

//TODO: Implementar Saga
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AgendamentoAutoProcessamento {
    private final AgendamentoService service;
    private final AgendamentoRepository repository;

    @Scheduled(cron = "0 0 */6 * * *") // a cada 6 horas
    @Transactional
    public void executar() {
        processarConfirmacoes();
        processarRevalidacoes();
    }

    public void processarConfirmacoes() {
        LocalDateTime limite = LocalDateTime.now().plusDays(7);
        List<AgendamentoOperacionalId> agendamentosConfirmar = repository.buscarIdEmAnaliseAte(limite);
        for (AgendamentoOperacionalId agendamento : agendamentosConfirmar) {
            try {
                ConfirmarAgendamento cmd = ConfirmarAgendamento.builder().id(agendamento).build();
                service.handle(cmd);
            } catch (Exception e) {
                ColocarAgendamentoEmConflito cmd = ColocarAgendamentoEmConflito.builder().id(agendamento).build();
                service.handle(cmd);
            }
        }
    }

    public void processarRevalidacoes() {
        LocalDateTime limite = LocalDateTime.now().plusDays(2);
        List<AgendamentoOperacionalId> agendamentosConfirmar = repository.buscarIdConfirmadosAte(limite);
        for (AgendamentoOperacionalId agendamento : agendamentosConfirmar) {
            try {
                RevalidarAgendamento cmd = RevalidarAgendamento.builder().id(agendamento).build();
                service.handle(cmd);
            } catch (Exception e) {
                ColocarAgendamentoEmConflito cmd = ColocarAgendamentoEmConflito.builder().id(agendamento).build();
                service.handle(cmd);
            }
        }
    }
}
