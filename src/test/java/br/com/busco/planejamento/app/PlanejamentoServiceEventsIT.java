package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.CancelarPlanejamento;
import br.com.busco.planejamento.app.cmd.DuplicarPlanejamento;
import br.com.busco.planejamento.app.cmd.EditarPlanejamento;
import br.com.busco.planejamento.app.cmd.PlanejarViagem;
import br.com.busco.planejamento.app.cmd.ValidarPlanejamento;
import br.com.busco.planejamento.domain.Planejamento;
import br.com.busco.planejamento.domain.PlanejamentoRepository;
import br.com.busco.planejamento.domain.Status;
import br.com.busco.planejamento.domain.events.PlanejamentoCancelado;
import br.com.busco.planejamento.domain.events.PlanejamentoEditado;
import br.com.busco.planejamento.domain.events.ViagemPlanejada;
import br.com.busco.planejamento.sk.ids.PlanejamentoId;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PlanejamentoService - eventos em vez de escrita direta")
class PlanejamentoServiceEventsIT {

    @TestConfiguration
    static class EventosConfig {
        @Bean
        ColetorDeEventos coletorDeEventos() {
            return new ColetorDeEventos();
        }
    }

    static class ColetorDeEventos {
        private final List<Object> eventos = new CopyOnWriteArrayList<>();

        @EventListener
        public void on(Object evento) {
            eventos.add(evento);
        }

        void limpar() {
            eventos.clear();
        }

        <T> Stream<T> porTipo(Class<T> tipo) {
            return eventos.stream().filter(tipo::isInstance).map(tipo::cast);
        }
    }

    @Autowired
    private PlanejamentoService service;

    @Autowired
    private PlanejamentoRepository repository;

    @Autowired
    private ColetorDeEventos events;

    private PlanejarViagem.PlanejarViagemBuilder baseCmd;

    @BeforeEach
    void setUp() {
        events.limpar();
        baseCmd = PlanejarViagem.builder()
                .dataInicio(LocalDate.now().plusDays(1))
                .dataFim(LocalDate.now().plusMonths(1))
                .diasSemana(List.of(DayOfWeek.MONDAY))
                .horaViagem(LocalTime.of(7, 30))
                .rota(UUID.randomUUID())
                .motorista(UUID.randomUUID())
                .veiculo(UUID.randomUUID())
                .monitor(UUID.randomUUID())
                .alunos(List.of(UUID.randomUUID()))
                .calendarios(List.of(UUID.randomUUID()))
                .grupoChecklistInicial(UUID.randomUUID())
                .grupoChecklistFinal(UUID.randomUUID());
    }

    @Test
    @DisplayName("Criar persiste em EM_ANALISE sem publicar evento de viagem")
    void criarNaoPublicaEvento() {
        UUID id = service.handle(baseCmd.build());

        Planejamento lido = repository.findById(PlanejamentoId.fromString(id.toString())).orElseThrow();

        assertThat(lido.getStatus()).isEqualTo(Status.EM_ANALISE);
        assertThat(events.porTipo(ViagemPlanejada.class)).isEmpty();
        assertThat(events.porTipo(PlanejamentoCancelado.class)).isEmpty();
    }

    @Test
    @DisplayName("Validar publica ViagemPlanejada (inclusive sem passageiros)")
    void validarPublicaEvento() {
        UUID id = service.handle(baseCmd.alunos(List.of()).build());

        service.handle(ValidarPlanejamento.builder().id(id).build());

        Planejamento lido = repository.findById(PlanejamentoId.fromString(id.toString())).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(Status.PLANEJADA);
        assertThat(events.porTipo(ViagemPlanejada.class)).hasSize(1);
    }

    @Test
    @DisplayName("Validar fora de EM_ANALISE preserva estado e não publica evento")
    void validarInvalidoPreservaEstado() {
        UUID id = service.handle(baseCmd.build());
        service.handle(ValidarPlanejamento.builder().id(id).build());
        events.limpar();

        assertThatThrownBy(() -> service.handle(ValidarPlanejamento.builder().id(id).build()))
                .isInstanceOf(IllegalStateException.class);

        Planejamento lido = repository.findById(PlanejamentoId.fromString(id.toString())).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(Status.PLANEJADA);
        assertThat(events.porTipo(ViagemPlanejada.class)).isEmpty();
    }

    @Test
    @DisplayName("Cancelar publica PlanejamentoCancelado")
    void cancelarPublicaEvento() {
        UUID id = service.handle(baseCmd.build());

        service.handle(CancelarPlanejamento.builder().id(id).build());

        Planejamento lido = repository.findById(PlanejamentoId.fromString(id.toString())).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(Status.CANCELADA);
        assertThat(events.porTipo(PlanejamentoCancelado.class)).hasSize(1);
    }

    @Test
    @DisplayName("Editar publica PlanejamentoEditado sem escrever em tabela de viagem")
    void editarPublicaEvento() {
        UUID id = service.handle(baseCmd.build());
        events.limpar();

        service.handle(EditarPlanejamento.builder()
                .id(id)
                .horaViagem(LocalTime.of(8, 0))
                .build());

        Planejamento lido = repository.findById(PlanejamentoId.fromString(id.toString())).orElseThrow();
        assertThat(lido.getHoraViagem()).isEqualTo(LocalTime.of(8, 0));
        assertThat(events.porTipo(PlanejamentoEditado.class)).hasSize(1);
    }

    @Test
    @DisplayName("Duplicar cria novo planejamento em EM_ANALISE")
    void duplicarCriaNovo() {
        UUID id = service.handle(baseCmd.build());

        UUID copiaId = service.handle(DuplicarPlanejamento.builder().id(id).build());

        assertThat(copiaId).isNotEqualTo(id);
        Planejamento copia = repository.findById(PlanejamentoId.fromString(copiaId.toString())).orElseThrow();
        assertThat(copia.getStatus()).isEqualTo(Status.EM_ANALISE);
        assertThat(copia.getRotaId()).isEqualTo(
                repository.findById(PlanejamentoId.fromString(id.toString())).orElseThrow().getRotaId());
    }

    @Test
    @DisplayName("Operação em planejamento inexistente lança EntityNotFoundException")
    void inexistenteLancaNotFound() {
        UUID inexistente = UUID.randomUUID();

        assertThatThrownBy(() -> service.handle(ValidarPlanejamento.builder().id(inexistente).build()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(inexistente.toString());
    }
}
