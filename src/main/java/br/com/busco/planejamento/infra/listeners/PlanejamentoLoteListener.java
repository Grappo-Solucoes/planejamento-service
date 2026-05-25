package br.com.busco.planejamento.infra.listeners;

import br.com.busco.planejamento.app.AgendamentoService;
import br.com.busco.planejamento.app.PlanejamentoService;
import br.com.busco.planejamento.app.cmd.CancelarAgendamentoPorLote;
import br.com.busco.planejamento.app.cmd.CriarAgendamento;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.domain.PlanejamentoLote;
import br.com.busco.planejamento.domain.events.PlanejamentoLoteCancelado;
import br.com.busco.planejamento.domain.events.PlanejamentoLoteConfirmado;
import br.com.busco.planejamento.domain.events.PlanejamentoLoteSuspenso;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Component
@AllArgsConstructor
public class PlanejamentoLoteListener {
    private final PlanejamentoService service;
    private final AgendamentoRepository repository;
    private final AgendamentoService agendamentoService;

    @Async
    @EventListener
    public void on(PlanejamentoLoteConfirmado evt) {
        PlanejamentoLote planejamentoLote = service.buscarPorId(evt.getId());

        List<LocalDateTime> datas = planejamentoLote.gerarDatas();
        datas.stream().map(data -> {
            return CriarAgendamento.builder()
                    .rota(planejamentoLote.getRota())
                    .motoristaPadrao(planejamentoLote.getMotorista())
                    .veiculoPadrao(planejamentoLote.getVeiculo())
                    .data(data)
                    .build();
        }).forEach(agendamentoService::handle);
    }

    @Async
    @EventListener
    public void on(PlanejamentoLoteCancelado evt) {
        List<AgendamentoOperacionalId> agendamentos = repository.buscarIdFuturosPorPlanejamento(evt.getId(), LocalDateTime.now());
        agendamentos.stream().map(agendamentoOperacionalId -> CancelarAgendamentoPorLote.builder().id(agendamentoOperacionalId).build())
                .forEach(agendamentoService::handle);
    }

    @Async
    @EventListener
    public void on(PlanejamentoLoteSuspenso evt) {
        List<AgendamentoOperacionalId> agendamentos = repository.buscarIdFuturosPorPlanejamento(evt.getId(), LocalDateTime.now());
        agendamentos.stream().map(agendamentoOperacionalId -> CancelarAgendamentoPorLote.builder().id(agendamentoOperacionalId).build())
                .forEach(agendamentoService::handle);
    }

}
