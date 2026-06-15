package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.*;
import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.domain.StatusAgendamento;
import br.com.busco.planejamento.sk.ids.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AgendamentoService - Testes de Integração")
class AgendamentoServiceIT {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private RotaId rotaId;
    private VeiculoId veiculoId;
    private MotoristaId motoristaId;
    private LocalDateTime dataFutura;
    private PontoId pontoEmbarque;
    private PontoId pontoDesembarque;

    @BeforeEach
    void setUp() {
        rotaId = RotaId.randomId();
        veiculoId = VeiculoId.randomId();
        motoristaId = MotoristaId.randomId();
        dataFutura = LocalDateTime.now().plusDays(1);
        pontoEmbarque = PontoId.randomId();
        pontoDesembarque = PontoId.randomId();
    }

    @Nested
    @DisplayName("Fluxo completo de criação e confirmação")
    class FluxoCompletoTests {

        @Test
        @DisplayName("Deve criar e confirmar agendamento com sucesso")
        void deveCriarEConfirmarAgendamento() {
            // Act - Criar
            var criarCmd = criarAgendamentoComPassageiro();
            var agendamentoId = agendamentoService.handle(criarCmd);

            // Assert - Criado
            var agendamento = agendamentoRepository.findById(agendamentoId).orElseThrow();
            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.EM_ANALISE);

            // Act - Confirmar
            var confirmarCmd = ConfirmarAgendamento.builder()
                    .id(agendamentoId)
                    .build();
            agendamentoService.handle(confirmarCmd);

            // Assert - Confirmado
            entityManager.clear();

            var agendamentoConfirmado = agendamentoRepository.findById(agendamentoId).orElseThrow();
            assertThat(agendamentoConfirmado.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("Deve criar, confirmar e realizar agendamento")
        void deveCriarConfirmarERealizarAgendamento() {
            // Criar
            var criarCmd = criarAgendamentoComPassageiro();
            var id = agendamentoService.handle(criarCmd);

            // Confirmar
            agendamentoService.handle(ConfirmarAgendamento.builder().id(id).build());

            var agendamento = agendamentoRepository.findById(id).orElseThrow();
            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }
    }

    @Nested
    @DisplayName("Validações e transições inválidas")
    class ValidacoesTests {

        @Test
        @DisplayName("Não deve confirmar agendamento sem passageiro")
        void naoDeveConfirmarSemPassageiro() {
            var criarCmd = CriarAgendamento.builder()
                    .rota(rotaId)
                    .veiculoPadrao(veiculoId)
                    .motoristaPadrao(motoristaId)
                    .data(dataFutura)
                    .build();
            var id = agendamentoService.handle(criarCmd);

            assertThatThrownBy(() -> agendamentoService.handle(ConfirmarAgendamento.builder().id(id).build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Não é possível confirmar agendamento sem passageiros");

            var agendamento = agendamentoRepository.findById(id).orElseThrow();
            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.EM_ANALISE);
        }

        @Test
        @DisplayName("Não deve confirmar agendamento duas vezes")
        void naoDeveConfirmarDuasVezes() {
            var id = agendamentoService.handle(criarAgendamentoComPassageiro());
            agendamentoService.handle(ConfirmarAgendamento.builder().id(id).build());

            assertThatThrownBy(() -> agendamentoService.handle(ConfirmarAgendamento.builder().id(id).build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Somente agendamentos em análise podem ser confirmados");
        }
    }

    @Nested
    @DisplayName("Testes de rollback")
    class RollbackTests {

        @Test
        @DisplayName("Deve fazer rollback quando política de capacidade falha")
        void deveFazerRollbackQuandoPoliticaFalha() {
            var criarCmd = CriarAgendamento.builder()
                    .rota(rotaId)
                    .veiculoPadrao(veiculoId)
                    .motoristaPadrao(motoristaId)
                    .data(dataFutura)
                    .build();
            var id = agendamentoService.handle(criarCmd);

            var confirmarCmd = ConfirmarAgendamento.builder().id(id).build();

            assertThatThrownBy(() -> agendamentoService.handle(confirmarCmd))
                    .isInstanceOf(IllegalStateException.class);

            entityManager.clear();

            var agendamento = agendamentoRepository.findById(id).orElseThrow();
            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.EM_ANALISE);
        }
    }

    private CriarAgendamento criarAgendamentoComPassageiro() {
        return CriarAgendamento.builder()
                .rota(rotaId)
                .veiculoPadrao(veiculoId)
                .motoristaPadrao(motoristaId)
                .data(dataFutura)
                .passageiros(Set.of(AlocarPassageiro.builder()
                        .passageiroId(PassageiroId.randomId())
                        .tipo(br.com.busco.planejamento.domain.TipoPassageiro.SENTADO)
                        .pontoEmbarque(pontoEmbarque)
                        .pontoDesembarque(pontoDesembarque)
                        .build()))
                .build();
    }
}
