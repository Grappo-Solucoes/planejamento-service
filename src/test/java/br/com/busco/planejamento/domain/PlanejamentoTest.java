package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.domain.events.PlanejamentoCancelado;
import br.com.busco.planejamento.domain.events.PlanejamentoEditado;
import br.com.busco.planejamento.domain.events.ViagemPlanejada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Planejamento - agregado desacoplado (paridade com o monólito)")
class PlanejamentoTest {

    private UUID rotaId;
    private UUID motoristaId;
    private UUID veiculoId;
    private UUID monitorId;

    @BeforeEach
    void setUp() {
        rotaId = UUID.randomUUID();
        motoristaId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        monitorId = UUID.randomUUID();
    }

    private Planejamento.PlanejamentoBuilder novoPlanejamento() {
        return Planejamento.builder()
                .dataInicio(LocalDate.now().plusDays(1))
                .dataFim(LocalDate.now().plusMonths(1))
                .diasDaSemana(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                .horaViagem(LocalTime.of(7, 30))
                .rotaId(rotaId)
                .motoristaId(motoristaId)
                .veiculoId(veiculoId)
                .monitorId(monitorId)
                .passageiros(List.of(
                        Passageiro.of(UUID.randomUUID(), "Ana", "Ponto A", true),
                        Passageiro.of(UUID.randomUUID(), "Beto", "Ponto B", false)))
                .calendarioIds(List.of(UUID.randomUUID()))
                .grupoChecklistInicialId(UUID.randomUUID())
                .grupoChecklistFinalId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Criação inicia em EM_ANALISE com referências só por ID")
    void criacaoEmAnalise() {
        Planejamento planejamento = novoPlanejamento().build();

        assertThat(planejamento.getStatus()).isEqualTo(Status.EM_ANALISE);
        assertThat(planejamento.getRotaId()).isEqualTo(rotaId);
        assertThat(planejamento.getMotoristaId()).isEqualTo(motoristaId);
        assertThat(planejamento.getVeiculoId()).isEqualTo(veiculoId);
        assertThat(planejamento.getMonitorId()).isEqualTo(monitorId);
        assertThat(planejamento.getPassageiros()).hasSize(2);
    }

    @Test
    @DisplayName("planejar() transita para PLANEJADA e registra ViagemPlanejada")
    void planejarTransitaERegistraEvento() {
        Planejamento planejamento = novoPlanejamento().build();

        planejamento.planejar();

        assertThat(planejamento.getStatus()).isEqualTo(Status.PLANEJADA);
        assertThat(planejamento.domainEventsForTest())
                .filteredOn(ViagemPlanejada.class::isInstance)
                .hasSize(1);
    }

    @Test
    @DisplayName("planejar() sem passageiros funciona (sem exigência de passageiro)")
    void planejarSemPassageiros() {
        Planejamento planejamento = novoPlanejamento().passageiros(List.of()).build();

        planejamento.planejar();

        assertThat(planejamento.getStatus()).isEqualTo(Status.PLANEJADA);
    }

    @Test
    @DisplayName("cancelar() transita para CANCELADA e registra PlanejamentoCancelado")
    void cancelarTransitaERegistraEvento() {
        Planejamento planejamento = novoPlanejamento().build();

        planejamento.cancelar();

        assertThat(planejamento.getStatus()).isEqualTo(Status.CANCELADA);
        assertThat(planejamento.domainEventsForTest())
                .filteredOn(PlanejamentoCancelado.class::isInstance)
                .hasSize(1);
    }

    @Test
    @DisplayName("update() aplica só campos presentes e registra PlanejamentoEditado")
    void updateParcial() {
        Planejamento planejamento = novoPlanejamento().build();
        LocalTime novaHora = LocalTime.of(8, 0);

        planejamento.update()
                .horaViagem(novaHora)
                .aplicar();

        assertThat(planejamento.getHoraViagem()).isEqualTo(novaHora);
        assertThat(planejamento.getRotaId()).isEqualTo(rotaId);
        assertThat(planejamento.getPassageiros()).hasSize(2);
        assertThat(planejamento.domainEventsForTest())
                .filteredOn(PlanejamentoEditado.class::isInstance)
                .hasSize(1);
    }

    @Test
    @DisplayName("getPassageirosAtivos() filtra inativos preservando o histórico")
    void passageirosAtivos() {
        Planejamento planejamento = novoPlanejamento().build();

        assertThat(planejamento.getPassageirosAtivos())
                .hasSize(1)
                .allMatch(Passageiro::isAtivo);
        assertThat(planejamento.getPassageiros()).hasSize(2);
    }
}
