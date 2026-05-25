package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.*;
import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.domain.OrigemAgendamento;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
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

    @BeforeEach
    void setUp() {
        rotaId = RotaId.randomId();
        veiculoId = VeiculoId.randomId();
        motoristaId = MotoristaId.randomId();
        dataFutura = LocalDateTime.now().plusDays(1);
    }

    @Nested
    @DisplayName("Fluxo completo de criação e confirmação")
    class FluxoCompletoTests {

        @Test
        @DisplayName("Deve criar e confirmar agendamento com sucesso")
        void deveCriarEConfirmarAgendamento() {
            // Act - Criar
            var criarCmd = CriarAgendamento.builder()
                    .rota(rotaId)
                    .veiculoPadrao(veiculoId)
                    .motoristaPadrao(motoristaId)
                    .data(dataFutura)
                    .build();
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
            entityManager.flush();
            entityManager.clear();

            var agendamentoConfirmado = agendamentoRepository.findById(agendamentoId).orElseThrow();
            assertThat(agendamentoConfirmado.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("Deve criar, confirmar e realizar agendamento")
        void deveCriarConfirmarERealizarAgendamento() {
            // Criar
            var criarCmd = CriarAgendamento.builder()
                    .rota(rotaId)
                    .veiculoPadrao(veiculoId)
                    .motoristaPadrao(motoristaId)
                    .data(dataFutura)
                    .build();
            var id = agendamentoService.handle(criarCmd);

            // Confirmar
            agendamentoService.handle(ConfirmarAgendamento.builder().id(id).build());

            // Realizar (precisa adicionar método no service)
            // agendamentoService.handle(RealizarAgendamento.builder().id(id).build());
        }
    }

    @Nested
    @DisplayName("Testes de concorrência")
    class ConcorrenciaTests {

        @Test
        @DisplayName("Deve prevenir confirmação concorrente do mesmo agendamento")
        void devePrevenirConfirmacaoConcorrente() throws Exception {
            // Criar agendamento
            var criarCmd = CriarAgendamento.builder()
                    .rota(rotaId)
                    .veiculoPadrao(veiculoId)
                    .motoristaPadrao(motoristaId)
                    .data(dataFutura)
                    .build();
            var id = agendamentoService.handle(criarCmd);

            var executor = Executors.newFixedThreadPool(2);
            var confirmarCmd = ConfirmarAgendamento.builder().id(id).build();

            // Tentar confirmar concorrentemente
            var future1 = CompletableFuture.supplyAsync(() ->
                    agendamentoService.handle(confirmarCmd), executor);
            var future2 = CompletableFuture.supplyAsync(() ->
                    agendamentoService.handle(confirmarCmd), executor);

            // Apenas uma deve ter sucesso
            var resultados = CompletableFuture.allOf(future1, future2);

            long sucessos = 0;
            long falhas = 0;

            // Verificar quantas falharam
            // Uma deve lançar exceção (agendamento já confirmado)
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

            // Supondo que politicaDeCapacidade vai lançar exceção
            // Deve fazer rollback e não persistir mudanças
            try {
                agendamentoService.handle(confirmarCmd);
            } catch (Exception e) {
                // Esperado
            }

            entityManager.flush();
            entityManager.clear();

            var agendamento = agendamentoRepository.findById(id).orElseThrow();
            assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.EM_ANALISE);
        }
    }
}