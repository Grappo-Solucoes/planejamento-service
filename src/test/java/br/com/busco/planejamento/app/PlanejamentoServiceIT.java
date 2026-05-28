package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.*;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.domain.Periodo;
import br.com.busco.planejamento.domain.PlanejamentoLote;
import br.com.busco.planejamento.domain.PlanejamentoLoteRepository;
import br.com.busco.planejamento.domain.StatusPlanejamentoLote;
import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PlanejamentoService - Testes de Integração")
class PlanejamentoServiceIT {

    @Autowired
    private PlanejamentoService planejamentoService;

    @Autowired
    private PlanejamentoLoteRepository planejamentoLoteRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    private RotaId rotaId;
    private VeiculoId veiculoId;
    private MotoristaId motoristaId;
    private LocalDateTime inicio;
    private LocalDateTime fim;

    @BeforeEach
    void setUp() {
        rotaId = RotaId.randomId();
        veiculoId = VeiculoId.randomId();
        motoristaId = MotoristaId.randomId();
        inicio = LocalDateTime.now().plusDays(1);
        fim = inicio.plusMonths(1);
    }

    @Nested
    @DisplayName("Ciclo de vida do planejamento")
    class CicloVidaTests {

        @Test
        @DisplayName("Deve criar, ativar, suspender e cancelar planejamento")
        void deveExecutarCicloCompleto() {
            // Criar
            var criarCmd = CriarPlanejamentoLote.builder()
                    .dataInicio(inicio)
                    .dataFim(fim)
                    .diasDaSemana(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .build();
            var id = planejamentoService.handle(criarCmd);

            // Verificar criação
            var planejamento = planejamentoService.buscarPorId(id);
            assertThat(planejamento.getStatus()).isEqualTo(StatusPlanejamentoLote.RASCUNHO);

            // Ativar
            planejamentoService.handle(ConfirmarPlanejamentoLote.builder().id(id).build());
            planejamento = planejamentoService.buscarPorId(id);
            assertThat(planejamento.getStatus()).isEqualTo(StatusPlanejamentoLote.ATIVO);
            assertThat(agendamentoRepository.buscarIdFuturosPorPlanejamento(id, LocalDateTime.now()))
                    .isNotEmpty();

            // Suspender
            planejamentoService.handle(SuspenderPlanejamentoLote.builder().id(id).build());
            planejamento = planejamentoService.buscarPorId(id);
            assertThat(planejamento.getStatus()).isEqualTo(StatusPlanejamentoLote.SUSPENSO);

            // Cancelar
            planejamentoService.handle(CancelarPlanejamentoLote.builder().id(id).build());
            planejamento = planejamentoService.buscarPorId(id);
            assertThat(planejamento.getStatus()).isEqualTo(StatusPlanejamentoLote.CANCELADO);
        }

        @Test
        @DisplayName("Não deve ativar planejamento já ativo")
        void naoDeveAtivarPlanejamentoJaAtivo() {
            var criarCmd = CriarPlanejamentoLote.builder()
                    .dataInicio(inicio)
                    .dataFim(fim)
                    .diasDaSemana(Set.of(DayOfWeek.MONDAY))
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .build();
            var id = planejamentoService.handle(criarCmd);

            planejamentoService.handle(ConfirmarPlanejamentoLote.builder().id(id).build());

            assertThatThrownBy(() ->
                    planejamentoService.handle(ConfirmarPlanejamentoLote.builder().id(id).build())
            ).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Geração de agendamentos")
    class GeracaoAgendamentosTests {

        @Test
        @DisplayName("Deve gerar agendamentos para todas as datas do período")
        void deveGerarAgendamentosParaTodasDatas() {
            LocalDate primeiraSegunda = LocalDate.now()
                    .plusDays(1)
                    .with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            var inicio = LocalDateTime.of(primeiraSegunda, LocalTime.of(10, 0));
            var fim = inicio.plusDays(6);

            var criarCmd = CriarPlanejamentoLote.builder()
                    .dataInicio(inicio)
                    .dataFim(fim)
                    .diasDaSemana(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .build();
            var id = planejamentoService.handle(criarCmd);

            planejamentoService.handle(ConfirmarPlanejamentoLote.builder().id(id).build());

            var agendamentos = agendamentoRepository.buscarIdFuturosPorPlanejamento(
                    id,
                    LocalDateTime.of(2023, 12, 31, 23, 59)
            );

            assertThat(agendamentos).hasSize(3);
        }

        @Test
        @DisplayName("Não deve confirmar planejamento suspenso")
        void naoDeveConfirmarPlanejamentoSuspenso() {
            var criarCmd = CriarPlanejamentoLote.builder()
                    .dataInicio(inicio)
                    .dataFim(fim)
                    .diasDaSemana(Set.of(DayOfWeek.MONDAY))
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .build();
            var id = planejamentoService.handle(criarCmd);

            planejamentoService.handle(ConfirmarPlanejamentoLote.builder().id(id).build());
            planejamentoService.handle(SuspenderPlanejamentoLote.builder().id(id).build());

            assertThatThrownBy(() -> planejamentoService.handle(ConfirmarPlanejamentoLote.builder().id(id).build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Somente rascunhos podem ser ativados");
        }
    }
}
