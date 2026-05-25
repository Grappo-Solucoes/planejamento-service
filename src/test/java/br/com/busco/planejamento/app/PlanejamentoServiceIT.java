package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PlanejamentoService - Testes de Integração")
class PlanejamentoServiceIT {

    @Autowired
    private PlanejamentoService planejamentoService;

    @Autowired
    private PlanejamentoLoteRepository planejamentoLoteRepository;

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
            var inicio = LocalDateTime.of(2024, 1, 1, 10, 0);  // Segunda
            var fim = LocalDateTime.of(2024, 1, 7, 10, 0);    // Domingo

            var criarCmd = CriarPlanejamentoLote.builder()
                    .dataInicio(inicio)
                    .dataFim(fim)
                    .diasDaSemana(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .build();
            var id = planejamentoService.handle(criarCmd);

            // TODO: Implementar método para gerar agendamentos
            // var agendamentos = planejamentoService.gerarAgendamentos(id);

            // Deveria gerar: 1(Seg), 3(Qua), 5(Sex)
            // assertThat(agendamentos).hasSize(3);
        }

        @Test
        @DisplayName("Não deve gerar agendamentos para planejamento suspenso")
        void naoDeveGerarAgendamentosParaPlanejamentoSuspenso() {
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

            // TODO: Verificar que não gera agendamentos quando suspenso
        }
    }
}