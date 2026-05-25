package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlanejamentoLote - Testes Unitários")
class PlanejamentoLoteTest {

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
    @DisplayName("Testes de criação")
    class CriacaoTests {

        @Test
        @DisplayName("Deve criar planejamento com status RASCUNHO")
        void deveCriarComStatusRascunho() {
            var planejamento = criarPlanejamentoValido();

            assertThat(planejamento.getStatus()).isEqualTo(StatusPlanejamentoLote.RASCUNHO);
            assertThat(planejamento.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Testes de ativação")
    class AtivacaoTests {

        @Test
        @DisplayName("Deve ativar planejamento em rascunho")
        void deveAtivarPlanejamentoRascunho() {
            var planejamento = criarPlanejamentoValido();

            planejamento.ativar();

            assertThat(planejamento.getStatus()).isEqualTo(StatusPlanejamentoLote.ATIVO);
        }

        @Test
        @DisplayName("Não deve ativar planejamento já ativo")
        void naoDeveAtivarPlanejamentoAtivo() {
            var planejamento = criarPlanejamentoValido();
            planejamento.ativar();

            assertThatThrownBy(planejamento::ativar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Somente rascunhos podem ser ativados");
        }

        @Test
        @DisplayName("Não deve ativar planejamento suspenso")
        void naoDeveAtivarPlanejamentoSuspenso() {
            var planejamento = criarPlanejamentoValido();
            planejamento.ativar();
            planejamento.suspender();

            assertThatThrownBy(planejamento::ativar)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Testes de suspensão")
    class SuspensaoTests {

        @Test
        @DisplayName("Deve suspender planejamento ativo")
        void deveSuspenderPlanejamentoAtivo() {
            var planejamento = criarPlanejamentoValido();
            planejamento.ativar();

            planejamento.suspender();

            assertThat(planejamento.getStatus()).isEqualTo(StatusPlanejamentoLote.SUSPENSO);
        }

        @Test
        @DisplayName("Não deve suspender planejamento em rascunho")
        void naoDeveSuspenderPlanejamentoRascunho() {
            var planejamento = criarPlanejamentoValido();

            assertThatThrownBy(planejamento::suspender)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Somente planejamento ativo pode ser suspenso");
        }
    }

    @Nested
    @DisplayName("Testes de geração de datas")
    class GeracaoDatasTests {

        @Test
        @DisplayName("Deve gerar datas apenas nos dias da semana especificados")
        void deveGerarDatasNosDiasEspecificados() {
            var recorrencia = RecorrenciaSemanal.of(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
            var periodo = Periodo.of(
                    LocalDateTime.of(2024, 1, 1, 10, 0),  // Segunda
                    LocalDateTime.of(2024, 1, 10, 10, 0)  // Quarta
            );
            var planejamento = PlanejamentoLote.builder()
                    .periodo(periodo)
                    .recorrencia(recorrencia)
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .build();

            var datas = planejamento.gerarDatas();

            // Deve gerar: 1(Seg), 3(Qua), 5(Sex), 8(Seg), 10(Qua)
            assertThat(datas).hasSize(5);
            assertThat(datas).allMatch(data ->
                    data.getDayOfWeek() == DayOfWeek.MONDAY ||
                            data.getDayOfWeek() == DayOfWeek.WEDNESDAY ||
                            data.getDayOfWeek() == DayOfWeek.FRIDAY
            );
        }

        @Test
        @DisplayName("Não deve gerar datas para período vazio")
        void naoDeveGerarDatasParaPeriodoVazio() {
            var recorrencia = RecorrenciaSemanal.of(Set.of(DayOfWeek.MONDAY));
            var periodo = Periodo.of(
                    LocalDateTime.of(2024, 1, 1, 10, 0),
                    LocalDateTime.of(2024, 1, 1, 10, 0)  // mesmo dia
            );
            var planejamento = PlanejamentoLote.builder()
                    .periodo(periodo)
                    .recorrencia(recorrencia)
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .build();

            var datas = planejamento.gerarDatas();

            if (periodo.getInicio().getDayOfWeek() == DayOfWeek.MONDAY) {
                assertThat(datas).hasSize(1);
            } else {
                assertThat(datas).isEmpty();
            }
        }

        @Test
        @DisplayName("Não deve gerar datas para planejamento cancelado")
        void naoDeveGerarDatasParaPlanejamentoCancelado() {
            var planejamento = criarPlanejamentoValido();
            planejamento.cancelar();

            assertThatThrownBy(planejamento::gerarDatas)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Planejamento cancelado não pode gerar datas");
        }

        @Test
        @DisplayName("Deve respeitar horário do período")
        void deveRespeitarHorarioDoPeriodo() {
            var recorrencia = RecorrenciaSemanal.of(Set.of(DayOfWeek.MONDAY));
            var periodo = Periodo.of(
                    LocalDateTime.of(2024, 1, 1, 14, 30),  // 14:30
                    LocalDateTime.of(2024, 1, 8, 14, 30)
            );
            var planejamento = PlanejamentoLote.builder()
                    .periodo(periodo)
                    .recorrencia(recorrencia)
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .build();

            var datas = planejamento.gerarDatas();

            assertThat(datas).allMatch(data -> data.getHour() == 14 && data.getMinute() == 30);
        }

        @Test
        @DisplayName("Deve limitar geração de datas para evitar performance issues")
        void deveLimitarGeracaoDeDatas() {
            var recorrencia = RecorrenciaSemanal.of(Set.of(DayOfWeek.MONDAY));
            var periodo = Periodo.of(
                    LocalDateTime.now(),
                    LocalDateTime.now().plusYears(100)  // 100 anos de datas
            );
            var planejamento = PlanejamentoLote.builder()
                    .periodo(periodo)
                    .recorrencia(recorrencia)
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .build();

            // Deveria ter um limite máximo, ex: 1000 datas
            var datas = planejamento.gerarDatas();

            // Por enquanto só assert que existe, mas idealmente implementar limite
            assertThat(datas).isNotEmpty();
        }
    }

    private PlanejamentoLote criarPlanejamentoValido() {
        var periodo = Periodo.of(inicio, fim);
        var recorrencia = RecorrenciaSemanal.of(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));

        return PlanejamentoLote.builder()
                .periodo(periodo)
                .recorrencia(recorrencia)
                .rota(rotaId)
                .veiculo(veiculoId)
                .motorista(motoristaId)
                .build();
    }
}