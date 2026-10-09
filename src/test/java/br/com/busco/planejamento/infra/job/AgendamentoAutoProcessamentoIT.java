package br.com.busco.planejamento.infra.job;

import br.com.busco.planejamento.app.AgendamentoService;
import br.com.busco.planejamento.app.cmd.AlocarPassageiro;
import br.com.busco.planejamento.app.cmd.ColocarAgendamentoEmConflito;
import br.com.busco.planejamento.app.cmd.ConfirmarAgendamento;
import br.com.busco.planejamento.app.cmd.CriarAgendamento;
import br.com.busco.planejamento.app.cmd.RevalidarAgendamento;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.domain.StatusAgendamento;
import br.com.busco.planejamento.domain.TipoPassageiro;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PassageiroId;
import br.com.busco.planejamento.sk.ids.PontoId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Prova o isolamento por item do auto-processamento (regressão do commit
 * {@code 8901397}): um item que falha não derruba o lote — o fallback
 * {@code EM_CONFLITO} commita em transação própria e os demais itens seguem
 * persistindo, sem {@code UnexpectedRollbackException}.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:autoprocessamentodb;MODE=PostgreSQL;"
        + "INIT=CREATE SCHEMA IF NOT EXISTS agendamento;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
@DisplayName("Auto-processamento - isolamento por item")
class AgendamentoAutoProcessamentoIT {

    @TestConfiguration
    static class JobConfig {

        @Bean
        AgendamentoAutoProcessamento agendamentoAutoProcessamento(
                AgendamentoService service,
                AgendamentoRepository repository,
                TransactionTemplate transactionTemplate) {
            return new AgendamentoAutoProcessamento(service, repository, transactionTemplate);
        }
    }

    @Autowired
    private AgendamentoAutoProcessamento job;

    @SpyBean
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
        dataFutura = LocalDateTime.now().plusDays(1).withNano(0);
    }

    @Test
    @DisplayName("Item com falha vira CONFLITO sem reverter os demais itens")
    void itemComFalhaNaoReverteOsDemais() {
        AgendamentoOperacionalId semPassageiro = criarSemPassageiro();
        AgendamentoOperacionalId comPassageiro = criarComPassageiro();

        assertThatCode(job::executar).doesNotThrowAnyException();

        entityManager.clear();

        assertThat(agendamentoRepository.findById(semPassageiro).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.CONFLITO);
        assertThat(agendamentoRepository.findById(comPassageiro).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    @DisplayName("Item cujo fallback lança não impede os demais de serem processados")
    void fallbackQueLancaNaoImpedeOsDemais() {
        AgendamentoOperacionalId semPassageiro = criarSemPassageiro();
        AgendamentoOperacionalId comPassageiro = criarComPassageiro();

        // O fallback (ColocarAgendamentoEmConflito) falha: antes, a exceção
        // escapava e abortava o loop. Agora é registrada e o item é ignorado.
        doThrow(new IllegalStateException("fallback indisponível"))
                .when(agendamentoService).handle(any(ColocarAgendamentoEmConflito.class));

        assertThatCode(job::executar).doesNotThrowAnyException();

        entityManager.clear();

        // O item cujo fallback falhou permanece EM_ANALISE (nenhum conflito aplicado)...
        assertThat(agendamentoRepository.findById(semPassageiro).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.EM_ANALISE);
        // ...e o item seguinte foi processado normalmente.
        assertThat(agendamentoRepository.findById(comPassageiro).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    @DisplayName("Revalidação não usa fallback de conflito e segue os demais itens")
    void revalidacaoNaoAplicaFallbackDeConflito() {
        AgendamentoOperacionalId falha = criarConfirmado();
        AgendamentoOperacionalId seguinte = criarConfirmado();

        ArgumentMatcher<RevalidarAgendamento> apenasFalha =
                cmd -> cmd != null && falha.equals(cmd.getId());
        doThrow(new IllegalStateException("revalidação indisponível"))
                .when(agendamentoService).handle(argThat(apenasFalha));

        assertThatCode(job::processarRevalidacoes).doesNotThrowAnyException();

        // Revalidação não tem fallback de conflito (os itens são CONFIRMADO).
        verify(agendamentoService, never()).handle(any(ColocarAgendamentoEmConflito.class));
        ArgumentMatcher<RevalidarAgendamento> apenasSeguinte =
                cmd -> cmd != null && seguinte.equals(cmd.getId());
        verify(agendamentoService, times(1)).handle(argThat(apenasSeguinte));

        entityManager.clear();
        assertThat(agendamentoRepository.findById(falha).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(agendamentoRepository.findById(seguinte).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    private AgendamentoOperacionalId criarConfirmado() {
        AgendamentoOperacionalId id = criarComPassageiro();
        agendamentoService.handle(ConfirmarAgendamento.builder().id(id).build());
        return id;
    }

    private AgendamentoOperacionalId criarSemPassageiro() {
        return agendamentoService.handle(CriarAgendamento.builder()
                .rota(rotaId)
                .veiculoPadrao(veiculoId)
                .motoristaPadrao(motoristaId)
                .data(dataFutura)
                .build());
    }

    private AgendamentoOperacionalId criarComPassageiro() {
        return agendamentoService.handle(CriarAgendamento.builder()
                .rota(rotaId)
                .veiculoPadrao(veiculoId)
                .motoristaPadrao(motoristaId)
                .data(dataFutura)
                .passageiros(Set.of(AlocarPassageiro.builder()
                        .passageiroId(PassageiroId.randomId())
                        .tipo(TipoPassageiro.SENTADO)
                        .pontoEmbarque(PontoId.randomId())
                        .pontoDesembarque(PontoId.randomId())
                        .build()))
                .build());
    }
}
