package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.domain.exceptions.SomenteAgendamentosEmLotePodemSerCancelados;
import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PassageiroId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.PontoId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AgendamentoOperacional - Testes Unitários")
class AgendamentoOperacionalTest {

    private RotaId rotaId;
    private VeiculoId veiculoId;
    private MotoristaId motoristaId;
    private LocalDateTime dataFutura;

    @BeforeEach
    void setUp() {
        rotaId = RotaId.randomId();
        veiculoId = VeiculoId.randomId();
        motoristaId = MotoristaId.randomId();
        dataFutura = LocalDateTime.now().plusDays(1);
    }

    @Nested
    @DisplayName("Testes de criação")
    class CriacaoTests {

        @Test
        @DisplayName("Deve criar agendamento com origem avulsa e status EM_ANALISE")
        void deveCriarAgendamentoComOrigemAvulsa() {
            var agendamento = AgendamentoOperacional.builder()
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .data(dataFutura)
                    .origem(OrigemAgendamento.avulso())
                    .build();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.EM_ANALISE);
            assertThat(agendamento.getOrigem().ehAvulso()).isTrue();
            assertThat(agendamento.getOrigem().ehDeLote()).isFalse();
            assertThat(agendamento.getId()).isNotNull();
        }

        @Test
        @DisplayName("Deve criar agendamento com origem de lote")
        void deveCriarAgendamentoComOrigemLote() {
            var planejamentoId = PlanejamentoLoteId.randomId();
            var agendamento = AgendamentoOperacional.builder()
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .data(dataFutura)
                    .origem(OrigemAgendamento.lote(planejamentoId))
                    .build();

            assertThat(agendamento.getOrigem().ehDeLote()).isTrue();
            assertThat(agendamento.getOrigem().getPlanejamentoId()).isEqualTo(planejamentoId);
        }


    }

    @Nested
    @DisplayName("Testes de confirmação")
    class ConfirmacaoTests {

        @Test
        @DisplayName("Deve confirmar agendamento em análise")
        void deveConfirmarAgendamentoEmAnalise() {
            var agendamento = criarAgendamentoValido();

            agendamento.confirmar();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("Não deve confirmar agendamento já confirmado")
        void naoDeveConfirmarAgendamentoConfirmado() {
            var agendamento = criarAgendamentoValido();
            agendamento.confirmar();

            assertThatThrownBy(agendamento::confirmar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Somente agendamentos em análise podem ser confirmados");
        }

        @Test
        @DisplayName("Não deve confirmar agendamento cancelado")
        void naoDeveConfirmarAgendamentoCancelado() {
            var agendamento = criarAgendamentoValido();
            agendamento.cancelar();

            assertThatThrownBy(agendamento::confirmar)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Não deve confirmar agendamento sem passageiros")
        void naoDeveConfirmarAgendamentoSemPassageiros() {
            var agendamento = criarAgendamentoSemPassageiro();

            assertThatThrownBy(agendamento::confirmar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Não é possível confirmar agendamento sem passageiros");
        }
    }

    @Nested
    @DisplayName("Testes de cancelamento")
    class CancelamentoTests {

        @Test
        @DisplayName("Deve cancelar agendamento em análise")
        void deveCancelarAgendamentoEmAnalise() {
            var agendamento = criarAgendamentoValido();

            agendamento.cancelar();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        }

        @Test
        @DisplayName("Deve cancelar agendamento confirmado via cancelar()")
        void deveCancelarAgendamentoConfirmado() {
            var agendamento = criarAgendamentoValido();
            agendamento.confirmar();

            agendamento.cancelar();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        }

        @Test
        @DisplayName("Não deve cancelar agendamento já cancelado")
        void naoDeveCancelarAgendamentoJaCancelado() {
            var agendamento = criarAgendamentoValido();
            agendamento.cancelar();

            assertThatThrownBy(agendamento::cancelar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Agendamento já cancelado");
        }

        @Test
        @DisplayName("Cancelamento por planejamento deve falhar para agendamento avulso")
        void cancelamentoPorPlanejamentoDeveFalharParaAvulso() {
            var agendamento = criarAgendamentoValido(); // origem avulsa

            assertThatThrownBy(agendamento::cancelarPorPlanejamento)
                    .isInstanceOf(SomenteAgendamentosEmLotePodemSerCancelados.class)
                    .hasMessage("Somente agendamentos de lote podem ser cancelados por planejamento");
        }

        @Test
        @DisplayName("Cancelamento por planejamento deve funcionar para agendamento de lote")
        void cancelamentoPorPlanejamentoParaLote() {
            var planejamentoId = PlanejamentoLoteId.randomId();
            var agendamento = AgendamentoOperacional.builder()
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .data(dataFutura)
                    .origem(OrigemAgendamento.lote(planejamentoId))
                    .build();
            alocarPassageiro(agendamento);
            agendamento.confirmar();

            agendamento.cancelarPorPlanejamento();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        }

        @Test
        @DisplayName("Não deve cancelar por planejamento agendamento realizado")
        void naoDeveCancelarPorPlanejamentoAgendamentoRealizado() {
            var planejamentoId = PlanejamentoLoteId.randomId();
            var agendamento = AgendamentoOperacional.builder()
                    .rota(rotaId)
                    .veiculo(veiculoId)
                    .motorista(motoristaId)
                    .data(dataFutura)
                    .origem(OrigemAgendamento.lote(planejamentoId))
                    .build();
            alocarPassageiro(agendamento);
            agendamento.confirmar();
            agendamento.realizar(); // FIXME: precisa corrigir o método realizar primeiro

            assertThatThrownBy(agendamento::cancelarPorPlanejamento)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Não é possível cancelar agendamento já realizado");
        }
    }

    @Nested
    @DisplayName("Testes de conflito")
    class ConflitoTests {

        @Test
        @DisplayName("Deve marcar agendamento em análise como conflito")
        void deveMarcarAgendamentoEmAnaliseComoConflito() {
            var agendamento = criarAgendamentoValido();

            agendamento.colocarEmConflito();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFLITO);
        }

        @Test
        @DisplayName("Não deve marcar agendamento cancelado como conflito")
        void naoDeveMarcarAgendamentoCanceladoComoConflito() {
            var agendamento = criarAgendamentoValido();
            agendamento.cancelar();

            assertThatThrownBy(agendamento::colocarEmConflito)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Somente agendamentos em análise podem ser marcados com conflito");
        }

        @Test
        @DisplayName("Não deve marcar agendamento confirmado como conflito")
        void naoDeveMarcarAgendamentoConfirmadoComoConflito() {
            var agendamento = criarAgendamentoValido();
            agendamento.confirmar();

            assertThatThrownBy(agendamento::colocarEmConflito)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Não deve permitir conflito duplicado")
        void naoDevePermitirConflitoDuplicado() {
            var agendamento = criarAgendamentoValido();
            agendamento.colocarEmConflito();

            // Deveria lançar exceção na segunda tentativa
            assertThatThrownBy(agendamento::colocarEmConflito)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Testes de realização")
    class RealizacaoTests {

        @Test
        @DisplayName("Deve realizar agendamento confirmado")
        void deveRealizarAgendamentoConfirmado() {
            var agendamento = criarAgendamentoValido();
            agendamento.confirmar();

            agendamento.realizar();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.REALIZADO);
        }

        @Test
        @DisplayName("Não deve realizar agendamento em análise")
        void naoDeveRealizarAgendamentoEmAnalise() {
            var agendamento = criarAgendamentoValido();

            assertThatThrownBy(agendamento::realizar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Somente confirmados podem ser realizados");
        }

        @Test
        @DisplayName("Não deve realizar agendamento já realizado duas vezes")
        void naoDeveRealizarAgendamentoDuasVezes() {
            var agendamento = criarAgendamentoValido();
            agendamento.confirmar();
            agendamento.realizar();

            assertThatThrownBy(agendamento::realizar)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Testes de revalidação")
    class RevalidacaoTests {

        @Test
        @DisplayName("Deve revalidar agendamento confirmado")
        void deveRevalidarAgendamentoConfirmado() {
            var agendamento = criarAgendamentoValido();
            agendamento.confirmar();

            agendamento.revalidar();

            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO); // status não muda
        }

        @Test
        @DisplayName("Não deve revalidar agendamento em análise")
        void naoDeveRevalidarAgendamentoEmAnalise() {
            var agendamento = criarAgendamentoValido();

            assertThatThrownBy(agendamento::revalidar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Somente confirmados podem ser revalidados");
        }
    }

    private AgendamentoOperacional criarAgendamentoValido() {
        var agendamento = criarAgendamentoSemPassageiro();
        alocarPassageiro(agendamento);
        return agendamento;
    }

    private AgendamentoOperacional criarAgendamentoSemPassageiro() {
        return AgendamentoOperacional.builder()
                .rota(rotaId)
                .veiculo(veiculoId)
                .motorista(motoristaId)
                .data(dataFutura)
                .origem(OrigemAgendamento.avulso())
                .build();
    }

    private void alocarPassageiro(AgendamentoOperacional agendamento) {
        agendamento.alocarPassageiro(
                PassageiroId.randomId(),
                PontoId.randomId(),
                PontoId.randomId(),
                TipoPassageiro.SENTADO
        );
    }
}
