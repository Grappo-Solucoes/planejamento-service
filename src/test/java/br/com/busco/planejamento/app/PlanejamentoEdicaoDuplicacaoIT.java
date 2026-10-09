package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.DuplicarPlanejamento;
import br.com.busco.planejamento.app.cmd.EditarPlanejamento;
import br.com.busco.planejamento.app.cmd.PlanejarViagem;
import br.com.busco.planejamento.domain.Passageiro;
import br.com.busco.planejamento.domain.Planejamento;
import br.com.busco.planejamento.domain.PlanejamentoRepository;
import br.com.busco.planejamento.domain.Status;
import br.com.busco.planejamento.sk.ids.PlanejamentoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PlanejamentoService - edição parcial e duplicação")
class PlanejamentoEdicaoDuplicacaoIT {

    @Autowired
    private PlanejamentoService service;

    @Autowired
    private PlanejamentoRepository repository;

    private UUID criar(boolean comAlunos) {
        return service.handle(PlanejarViagem.builder()
                .dataInicio(LocalDate.now().plusDays(1))
                .dataFim(LocalDate.now().plusMonths(1))
                .diasSemana(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .horaViagem(LocalTime.of(7, 30))
                .rota(UUID.randomUUID())
                .motorista(UUID.randomUUID())
                .veiculo(UUID.randomUUID())
                .monitor(UUID.randomUUID())
                .alunos(comAlunos ? List.of(UUID.randomUUID(), UUID.randomUUID()) : List.of())
                .calendarios(List.of(UUID.randomUUID()))
                .grupoChecklistInicial(UUID.randomUUID())
                .grupoChecklistFinal(UUID.randomUUID())
                .build());
    }

    private Planejamento porId(UUID id) {
        return repository.findById(PlanejamentoId.fromString(id.toString())).orElseThrow();
    }

    @Test
    @DisplayName("Edição parcial altera só os campos enviados")
    void edicaoParcialPreservaNaoEnviados() {
        UUID id = criar(true);
        Planejamento antes = porId(id);

        service.handle(EditarPlanejamento.builder()
                .id(id)
                .horaViagem(LocalTime.of(9, 15))
                .build());

        Planejamento depois = porId(id);
        assertThat(depois.getHoraViagem()).isEqualTo(LocalTime.of(9, 15));
        assertThat(depois.getDataInicio()).isEqualTo(antes.getDataInicio());
        assertThat(depois.getDataFim()).isEqualTo(antes.getDataFim());
        assertThat(depois.getDiasDaSemana()).containsExactlyElementsOf(antes.getDiasDaSemana());
        assertThat(depois.getRotaId()).isEqualTo(antes.getRotaId());
        assertThat(depois.getMotoristaId()).isEqualTo(antes.getMotoristaId());
        assertThat(depois.getVeiculoId()).isEqualTo(antes.getVeiculoId());
        assertThat(depois.getMonitorId()).isEqualTo(antes.getMonitorId());
        assertThat(depois.getPassageiros()).hasSize(2);
        assertThat(depois.getCalendarioIds()).containsExactlyElementsOf(antes.getCalendarioIds());
        assertThat(depois.getStatus()).isEqualTo(Status.EM_ANALISE);
    }

    @Test
    @DisplayName("Edição troca equipe e lista de alunos quando enviados")
    void edicaoTrocaEquipeEAlunos() {
        UUID id = criar(true);
        UUID novoMotorista = UUID.randomUUID();
        UUID novoAluno = UUID.randomUUID();

        service.handle(EditarPlanejamento.builder()
                .id(id)
                .motorista(novoMotorista)
                .alunos(List.of(novoAluno))
                .build());

        Planejamento depois = porId(id);
        assertThat(depois.getMotoristaId()).isEqualTo(novoMotorista);
        assertThat(depois.getPassageiros())
                .hasSize(1)
                .first()
                .matches(p -> p.getPassageiroId().equals(novoAluno));
    }

    @Test
    @DisplayName("Edição full-payload (formato monólito) aplica todos os campos")
    void edicaoFullPayloadAplicaTudo() {
        UUID id = criar(true);
        LocalDate novaInicio = LocalDate.now().plusDays(5);
        LocalDate novaFim = LocalDate.now().plusMonths(2);
        UUID novaRota = UUID.randomUUID();
        UUID novoMotorista = UUID.randomUUID();
        UUID novoVeiculo = UUID.randomUUID();
        UUID novoMonitor = UUID.randomUUID();
        UUID novoAluno = UUID.randomUUID();
        UUID novoCalendario = UUID.randomUUID();
        UUID novoCheckIni = UUID.randomUUID();
        UUID novoCheckFim = UUID.randomUUID();

        service.handle(EditarPlanejamento.builder()
                .id(id)
                .dataInicio(novaInicio)
                .dataFim(novaFim)
                .horaViagem(LocalTime.of(10, 30))
                .diasSemana(List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))
                .calendarios(List.of(novoCalendario))
                .rota(novaRota)
                .motorista(novoMotorista)
                .veiculo(novoVeiculo)
                .monitor(novoMonitor)
                .alunos(List.of(novoAluno))
                .grupoChecklistInicial(novoCheckIni)
                .grupoChecklistFinal(novoCheckFim)
                .build());

        Planejamento depois = porId(id);
        assertThat(depois.getDataInicio()).isEqualTo(novaInicio);
        assertThat(depois.getDataFim()).isEqualTo(novaFim);
        assertThat(depois.getHoraViagem()).isEqualTo(LocalTime.of(10, 30));
        assertThat(depois.getDiasDaSemana()).containsExactly(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
        assertThat(depois.getCalendarioIds()).containsExactly(novoCalendario);
        assertThat(depois.getRotaId()).isEqualTo(novaRota);
        assertThat(depois.getMotoristaId()).isEqualTo(novoMotorista);
        assertThat(depois.getVeiculoId()).isEqualTo(novoVeiculo);
        assertThat(depois.getMonitorId()).isEqualTo(novoMonitor);
        assertThat(depois.getPassageiros())
                .hasSize(1)
                .first()
                .matches(p -> p.getPassageiroId().equals(novoAluno));
        assertThat(depois.getGrupoChecklistInicialId()).isEqualTo(novoCheckIni);
        assertThat(depois.getGrupoChecklistFinalId()).isEqualTo(novoCheckFim);
    }

    @Test
    @DisplayName("Duplicar copia agenda/equipe/veículo/rota e só alunos ativos, em EM_ANALISE")
    void duplicarCopiaAtivos() {
        UUID ativoId = UUID.randomUUID();
        UUID inativoId = UUID.randomUUID();
        Planejamento origem = Planejamento.builder()
                .dataInicio(LocalDate.now().plusDays(1))
                .dataFim(LocalDate.now().plusMonths(1))
                .diasDaSemana(List.of(DayOfWeek.TUESDAY))
                .horaViagem(LocalTime.of(6, 45))
                .rotaId(UUID.randomUUID())
                .motoristaId(UUID.randomUUID())
                .veiculoId(UUID.randomUUID())
                .monitorId(UUID.randomUUID())
                .passageiros(List.of(
                        Passageiro.of(ativoId, "Ana", "Ponto A", true),
                        Passageiro.of(inativoId, "Beto", "Ponto B", false)))
                .calendarioIds(List.of(UUID.randomUUID()))
                .grupoChecklistInicialId(UUID.randomUUID())
                .grupoChecklistFinalId(UUID.randomUUID())
                .codigo("origem")
                .build();
        UUID origemId = UUID.fromString(repository.save(origem).getId().toUUID());

        UUID copiaId = service.handle(DuplicarPlanejamento.builder().id(origemId).build());

        Planejamento copia = porId(copiaId);
        Planejamento origemLida = porId(origemId);
        assertThat(copiaId).isNotEqualTo(origemId);
        assertThat(copia.getStatus()).isEqualTo(Status.EM_ANALISE);
        assertThat(copia.getDataInicio()).isEqualTo(origemLida.getDataInicio());
        assertThat(copia.getDataFim()).isEqualTo(origemLida.getDataFim());
        assertThat(copia.getDiasDaSemana()).containsExactlyElementsOf(origemLida.getDiasDaSemana());
        assertThat(copia.getHoraViagem()).isEqualTo(origemLida.getHoraViagem());
        assertThat(copia.getRotaId()).isEqualTo(origemLida.getRotaId());
        assertThat(copia.getMotoristaId()).isEqualTo(origemLida.getMotoristaId());
        assertThat(copia.getVeiculoId()).isEqualTo(origemLida.getVeiculoId());
        assertThat(copia.getMonitorId()).isEqualTo(origemLida.getMonitorId());
        assertThat(copia.getPassageiros())
                .hasSize(1)
                .first()
                .matches(p -> p.getPassageiroId().equals(ativoId) && "Ana".equals(p.getNome()));
    }
}
