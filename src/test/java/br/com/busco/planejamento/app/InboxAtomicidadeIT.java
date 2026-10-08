package br.com.busco.planejamento.app;

import br.com.busco.planejamento.domain.PlanejamentoLoteRepository;
import br.com.busco.planejamento.infra.events.EnvelopeEvento;
import br.com.busco.planejamento.infra.events.EventosV1;
import br.com.busco.planejamento.infra.inbox.ProcessedEvent;
import br.com.busco.planejamento.infra.inbox.ProcessedEventRepository;
import br.com.busco.planejamento.infra.outbox.OutboxEvent;
import br.com.busco.planejamento.infra.outbox.OutboxEventRepository;
import br.com.busco.planejamento.sk.ddd.TenantContext;
import br.com.busco.planejamento.sk.ddd.TenantId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * Prova a atomicidade do inbox (CONTRACT §6): estado + outbox + marcador de
 * {@code processed_events} commitam na mesma transação. Se o registro do
 * marcador falhar (ex.: duplicata concorrente), nada do comando permanece;
 * reentrega após sucesso não reaplica o efeito.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Atomicidade do inbox idempotente")
class InboxAtomicidadeIT {

    private static final String TENANT = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private ProcessamentoComandoService processamentoComandoService;

    @Autowired
    private PlanejamentoLoteRepository planejamentoLoteRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @SpyBean
    private ProcessedEventRepository processedEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void definirTenant() {
        TenantContext.set(TenantId.fromString(TENANT));
    }

    @AfterEach
    void limparTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Falha ao registrar o marcador não deixa estado nem outbox aplicados")
    void falhaNoMarcadorNaoDeixaEstadoAplicado() {
        String loteId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        EnvelopeEvento envelope = envelopeCriar(eventId, loteId);

        doThrow(new DataIntegrityViolationException("marcador duplicado (corrida simulada)"))
                .when(processedEventRepository).save(any(ProcessedEvent.class));

        assertThatThrownBy(() -> processamentoComandoService.processar(envelope))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Nada do comando pode ter sido persistido: rollback conjunto.
        assertThat(planejamentoLoteRepository.findById(PlanejamentoLoteId.fromString(loteId))).isEmpty();
        assertThat(outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId)).isEmpty();
        assertThat(processedEventRepository.existsById(eventId)).isFalse();

        // Reentrega com o marcador funcionando aplica o comando uma única vez.
        reset(processedEventRepository);
        processamentoComandoService.processar(envelope);

        assertThat(planejamentoLoteRepository.findById(PlanejamentoLoteId.fromString(loteId))).isPresent();
        assertThat(outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId)).hasSize(1);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("Reentrega após sucesso é confirmada sem reaplicar o efeito")
    void reentregaAposSucessoNaoReaplica() {
        String loteId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        EnvelopeEvento envelope = envelopeCriar(eventId, loteId);

        processamentoComandoService.processar(envelope);

        assertThat(planejamentoLoteRepository.findById(PlanejamentoLoteId.fromString(loteId))).isPresent();
        assertThat(outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId)).hasSize(1);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();

        // Mesmo eventId novamente: retorno imediato, sem novo efeito.
        processamentoComandoService.processar(envelope);

        assertThat(outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId)).hasSize(1);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("Marcador duplicado falha como violação de integridade (INSERT, sem UPDATE silencioso)")
    void marcadorDuplicadoFalhaComoViolacaoDeIntegridade() {
        String eventId = UUID.randomUUID().toString();
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType("tipo-original")
                .processedAt(LocalDateTime.now())
                .build());

        assertThatThrownBy(() -> processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType("tipo-sobrescrito")
                .processedAt(LocalDateTime.now())
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(processedEventRepository.findById(eventId).orElseThrow().getEventType())
                .isEqualTo("tipo-original");
    }

    @Test
    @DisplayName("Registro tardio do marcador (check de inbox perdido) colide e reverte o comando")
    void registroTardioDoMarcadorColideEReverteOComando() {
        String loteId = UUID.randomUUID().toString();
        processamentoComandoService.processar(envelopeCriar(UUID.randomUUID().toString(), loteId));

        String eventIdEditar = UUID.randomUUID().toString();
        EnvelopeEvento editar = envelopeEditar(eventIdEditar, loteId);
        processamentoComandoService.processar(editar);
        assertThat(outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId))
                .extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("planejamento.lote.criado", "planejamento.lote.editado");

        // Perdedor que passou pelo existsById antes do commit do vencedor e
        // registra o marcador depois: o INSERT colide e nada é reaplicado.
        doReturn(false).when(processedEventRepository).existsById(eventIdEditar);
        assertThatThrownBy(() -> processamentoComandoService.processar(editar))
                .isInstanceOf(RuntimeException.class);
        reset(processedEventRepository);

        assertThat(outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId))
                .extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("planejamento.lote.criado", "planejamento.lote.editado");
        assertThat(processedEventRepository.findById(eventIdEditar)).isPresent();
    }

    @Test
    @DisplayName("Entregas concorrentes do mesmo eventId aplicam o efeito uma única vez")
    void entregasConcorrentesDoMesmoEventoAplicamUmaVez() throws Exception {
        String loteId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        EnvelopeEvento envelope = envelopeCriar(eventId, loteId);

        int entregas = 4;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(entregas);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < entregas; i++) {
                resultados.add(executor.submit(() -> {
                    largada.await();
                    try {
                        processamentoComandoService.processar(envelope);
                        return true;
                    } catch (RuntimeException e) {
                        return false;
                    }
                }));
            }
            largada.countDown();
            for (Future<Boolean> resultado : resultados) {
                resultado.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        // Reentrega após a corrida: marcador presente → ack sem novo efeito.
        processamentoComandoService.processar(envelope);

        assertThat(planejamentoLoteRepository.findById(PlanejamentoLoteId.fromString(loteId))).isPresent();
        assertThat(outboxEventRepository.findByCorrelationIdOrderByCreatedAtAsc(loteId))
                .extracting(OutboxEvent::getEventType)
                .containsExactly("planejamento.lote.criado");
        assertThat(processedEventRepository.findById(eventId)).isPresent();
    }

    private EnvelopeEvento envelopeCriar(String eventId, String loteId) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withNano(0);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("loteId", loteId);
        payload.put("rotaId", UUID.randomUUID().toString());
        payload.put("veiculoId", UUID.randomUUID().toString());
        payload.put("motoristaId", UUID.randomUUID().toString());
        payload.put("dataInicio", inicio.toString());
        payload.put("dataFim", inicio.plusMonths(1).toString());
        payload.putArray("diasDaSemana").add(1).add(3).add(5);
        return new EnvelopeEvento(eventId, EventosV1.LOTE_CRIAR_SOLICITADO, EventosV1.VERSAO,
                Instant.now(), loteId, TENANT, payload);
    }

    private EnvelopeEvento envelopeEditar(String eventId, String loteId) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withNano(0);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("loteId", loteId);
        payload.put("rotaId", UUID.randomUUID().toString());
        payload.put("veiculoId", UUID.randomUUID().toString());
        payload.put("motoristaId", UUID.randomUUID().toString());
        payload.put("dataInicio", inicio.toString());
        payload.put("dataFim", inicio.plusMonths(1).toString());
        payload.putArray("diasDaSemana").add(2);
        return new EnvelopeEvento(eventId, EventosV1.LOTE_EDITAR_SOLICITADO, EventosV1.VERSAO,
                Instant.now(), loteId, TENANT, payload);
    }
}
