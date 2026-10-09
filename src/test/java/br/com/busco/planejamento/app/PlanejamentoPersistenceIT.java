package br.com.busco.planejamento.app;

import br.com.busco.planejamento.domain.Passageiro;
import br.com.busco.planejamento.domain.Planejamento;
import br.com.busco.planejamento.domain.PlanejamentoRepository;
import br.com.busco.planejamento.domain.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Planejamento - persistência desacoplada (sem FK externa)")
class PlanejamentoPersistenceIT {

    @Autowired
    private PlanejamentoRepository repository;

    @Test
    @DisplayName("Persiste com referências opacas e VOs, sem exigir tabelas de cadastro")
    void persisteSemFkExterna() {
        UUID rotaId = UUID.randomUUID();
        UUID alunoAtivo = UUID.randomUUID();

        Planejamento planejamento = Planejamento.builder()
                .dataInicio(LocalDate.now().plusDays(1))
                .dataFim(LocalDate.now().plusMonths(1))
                .diasDaSemana(List.of(DayOfWeek.MONDAY))
                .horaViagem(LocalTime.of(7, 30))
                .rotaId(rotaId)
                .motoristaId(UUID.randomUUID())
                .veiculoId(UUID.randomUUID())
                .monitorId(UUID.randomUUID())
                .passageiros(List.of(
                        Passageiro.of(alunoAtivo, "Ana", "Ponto A", true),
                        Passageiro.of(UUID.randomUUID(), "Beto", "Ponto B", false)))
                .calendarioIds(List.of(UUID.randomUUID()))
                .grupoChecklistInicialId(UUID.randomUUID())
                .grupoChecklistFinalId(UUID.randomUUID())
                .codigo("PLJ-1")
                .build();

        Planejamento salvo = repository.save(planejamento);

        Planejamento lido = repository.findById(salvo.getId()).orElseThrow();

        assertThat(lido.getStatus()).isEqualTo(Status.EM_ANALISE);
        assertThat(lido.getRotaId()).isEqualTo(rotaId);
        assertThat(lido.getPassageiros()).hasSize(2);
        assertThat(lido.getPassageirosAtivos())
                .hasSize(1)
                .first()
                .matches(p -> p.getPassageiroId().equals(alunoAtivo)
                        && "Ana".equals(p.getNome())
                        && "Ponto A".equals(p.getPonto()));
        assertThat(lido.getCalendarioIds()).hasSize(1);
    }
}
