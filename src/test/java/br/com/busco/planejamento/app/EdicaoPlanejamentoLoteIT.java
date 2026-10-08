package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.ConfirmarPlanejamentoLote;
import br.com.busco.planejamento.app.cmd.CriarPlanejamentoLote;
import br.com.busco.planejamento.app.cmd.EditarPlanejamentoLote;
import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.domain.StatusAgendamento;
import br.com.busco.planejamento.domain.StatusPlanejamentoLote;
import br.com.busco.planejamento.domain.TipoPassageiro;
import br.com.busco.planejamento.infra.outbox.OutboxEvent;
import br.com.busco.planejamento.infra.outbox.OutboxEventRepository;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PassageiroId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.PontoId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Edição de lote - Testes de Integração")
class EdicaoPlanejamentoLoteIT {

    @Autowired
    private PlanejamentoService planejamentoService;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("Edição de lote ativo cancela futuros não executados, re-expande e preserva REALIZADO")
    void deveCancelarFuturosEReExpandirPreservandoRealizado() {
        RotaId rotaOriginal = RotaId.randomId();
        VeiculoId veiculo = VeiculoId.randomId();
        MotoristaId motorista = MotoristaId.randomId();

        LocalDate primeiraSegunda = LocalDate.now().plusDays(14)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDateTime inicio = LocalDateTime.of(primeiraSegunda, LocalTime.of(10, 0));
        LocalDateTime fim = inicio.plusDays(6);

        PlanejamentoLoteId loteId = planejamentoService.handle(CriarPlanejamentoLote.builder()
                .dataInicio(inicio)
                .dataFim(fim)
                .diasDaSemana(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                .rota(rotaOriginal)
                .veiculo(veiculo)
                .motorista(motorista)
                .build());

        planejamentoService.handle(ConfirmarPlanejamentoLote.builder().id(loteId).build());

        List<AgendamentoOperacionalId> idsOriginais = buscarTodos(loteId);
        assertThat(idsOriginais).hasSize(3);

        // marca o agendamento da segunda-feira como REALIZADO
        AgendamentoOperacionalId idRealizado = idsOriginais.stream()
                .filter(id -> agendamentoRepository.findById(id).orElseThrow().getData().getDayOfWeek() == DayOfWeek.MONDAY)
                .findFirst()
                .orElseThrow();
        AgendamentoOperacional realizado = agendamentoRepository.findById(idRealizado).orElseThrow();
        realizado.alocarPassageiro(PassageiroId.randomId(), PontoId.randomId(), PontoId.randomId(), TipoPassageiro.SENTADO);
        realizado.confirmar();
        realizado.realizar();
        agendamentoRepository.save(realizado);

        // edita: nova rota, mesmo período, apenas sexta-feira
        RotaId rotaNova = RotaId.randomId();
        planejamentoService.handle(EditarPlanejamentoLote.builder()
                .id(loteId)
                .dataInicio(inicio)
                .dataFim(fim)
                .diasDaSemana(Set.of(DayOfWeek.FRIDAY))
                .rota(rotaNova)
                .veiculo(veiculo)
                .motorista(motorista)
                .build());

        // lote atualizado e ainda ATIVO
        var lote = planejamentoService.buscarPorId(loteId);
        assertThat(lote.getStatus()).isEqualTo(StatusPlanejamentoLote.ATIVO);
        assertThat(lote.getRota()).isEqualTo(rotaNova);

        List<AgendamentoOperacional> todos = buscarTodos(loteId).stream()
                .map(id -> agendamentoRepository.findById(id).orElseThrow())
                .toList();

        // REALIZADO intocado
        var preservado = todos.stream().filter(a -> a.getId().equals(idRealizado)).findFirst().orElseThrow();
        assertThat(preservado.getStatus()).isEqualTo(StatusAgendamento.REALIZADO);

        // futuros não executados da agenda antiga (quarta e sexta) cancelados
        var antigos = todos.stream().filter(a -> a.getRota().equals(rotaOriginal)).toList();
        assertThat(antigos).hasSize(3);
        assertThat(antigos.stream().filter(a -> a.getStatus() == StatusAgendamento.CANCELADO)).hasSize(2);
        assertThat(antigos.stream().filter(a -> a.getStatus() == StatusAgendamento.REALIZADO)).hasSize(1);
        assertThat(antigos).noneMatch(a -> a.getStatus() == StatusAgendamento.EM_ANALISE);

        // re-expansão cria a sexta-feira da nova agenda
        var novos = todos.stream().filter(a -> a.getRota().equals(rotaNova)).toList();
        assertThat(novos).hasSize(1);
        assertThat(novos.get(0).getData())
                .isEqualTo(LocalDateTime.of(primeiraSegunda.plusDays(4), LocalTime.of(10, 0)));
        assertThat(novos.get(0).getStatus()).isEqualTo(StatusAgendamento.EM_ANALISE);

        // eventos correspondentes gravados na outbox (CONTRACT §5)
        var eventos = outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId.toUUID());
        var porTipo = eventos.stream()
                .collect(Collectors.groupingBy(OutboxEvent::getEventType, Collectors.counting()));
        assertThat(porTipo)
                .containsEntry("planejamento.lote.criado", 1L)
                .containsEntry("planejamento.lote.confirmado", 1L)
                .containsEntry("planejamento.lote.editado", 1L)
                .containsEntry("agendamento.criado", 4L) // 3 na confirmação + 1 na re-expansão
                .containsEntry("agendamento.cancelado", 2L);
    }

    private List<AgendamentoOperacionalId> buscarTodos(PlanejamentoLoteId loteId) {
        return agendamentoRepository.buscarIdFuturosPorPlanejamento(loteId, LocalDateTime.of(2023, 12, 31, 23, 59));
    }
}
