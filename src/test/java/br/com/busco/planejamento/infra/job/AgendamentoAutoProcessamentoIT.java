package br.com.busco.planejamento.infra.job;

import br.com.busco.planejamento.app.AgendamentoService;
import br.com.busco.planejamento.app.cmd.AlocarPassageiro;
import br.com.busco.planejamento.app.cmd.CriarAgendamento;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
