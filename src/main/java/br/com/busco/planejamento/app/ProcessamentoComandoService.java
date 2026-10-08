package br.com.busco.planejamento.app;

import br.com.busco.planejamento.app.cmd.CancelarPlanejamentoLote;
import br.com.busco.planejamento.app.cmd.ConfirmarPlanejamentoLote;
import br.com.busco.planejamento.app.cmd.CriarPlanejamentoLote;
import br.com.busco.planejamento.app.cmd.EditarPlanejamentoLote;
import br.com.busco.planejamento.infra.events.EnvelopeEvento;
import br.com.busco.planejamento.infra.events.EventosV1;
import br.com.busco.planejamento.infra.events.IntegrationEvent;
import br.com.busco.planejamento.infra.events.MensagemNaoProcessavelException;
import br.com.busco.planejamento.infra.inbox.ProcessedEvent;
import br.com.busco.planejamento.infra.inbox.ProcessedEventRepository;
import br.com.busco.planejamento.infra.outbox.OutboxRecorder;
import br.com.busco.planejamento.sk.ddd.TenantProvider;
import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Processa um comando do catálogo v1 (CONTRACT §4.1/§5): valida o envelope,
 * aplica o comando via {@link PlanejamentoService} — que publica os eventos de
 * sucesso pela outbox — e, quando o comando é rejeitado por regra de negócio,
 * publica {@code planejamento.lote.falha} com o motivo. Todo comando do
 * catálogo produz exatamente um resultado.
 *
 * <p>A aplicação do comando e o marcador de inbox commitam na mesma transação;
 * quando o comando é rejeitado, essa transação reverte por completo e o
 * resultado de falha + marcador commitam juntos em uma segunda transação
 * (evita o rollback-only propagado pela exceção de negócio). Falha do
 * marcador (ex.: colisão de {@code eventId} em entrega duplicada) não é
 * rejeição de negócio: propaga como infraestrutura para retry/DLQ, sem
 * {@code lote.falha} espúrio.</p>
 *
 * <p>Falhas de negócio são confirmadas (ack); mensagens malformadas ou fora do
 * catálogo de comandos lançam {@link MensagemNaoProcessavelException} e seguem
 * para a DLQ após o retry; falhas de infraestrutura são relançadas para
 * reprocessamento.</p>
 */
@Slf4j
@Service
public class ProcessamentoComandoService {

    private static final int TAMANHO_MAX_MOTIVO = 500;

    private final PlanejamentoService planejamentoService;
    private final OutboxRecorder outboxRecorder;
    private final TenantProvider tenantProvider;
    private final ProcessedEventRepository processedEventRepository;
    private final TransactionTemplate transactionTemplate;

    public ProcessamentoComandoService(
            PlanejamentoService planejamentoService,
            OutboxRecorder outboxRecorder,
            TenantProvider tenantProvider,
            ProcessedEventRepository processedEventRepository,
            PlatformTransactionManager transactionManager) {
        this.planejamentoService = planejamentoService;
        this.outboxRecorder = outboxRecorder;
        this.tenantProvider = tenantProvider;
        this.processedEventRepository = processedEventRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void processar(EnvelopeEvento envelope) {
        validar(envelope);

        if (processedEventRepository.existsById(envelope.eventId())) {
            log.info("Evento {} ({}) já processado; reentrega confirmada sem efeito",
                    envelope.eventId(), envelope.eventType());
            return;
        }

        try {
            // Sucesso: estado + outbox + marcador de inbox na mesma transação.
            transactionTemplate.executeWithoutResult(status -> {
                despacharTraduzindoRejeicao(envelope);
                registrarProcessado(envelope);
            });
        } catch (RejeicaoDeNegocio rejeicao) {
            RuntimeException causa = (RuntimeException) rejeicao.getCause();
            log.warn("Comando {} rejeitado: {}", envelope.eventType(), causa.getMessage());
            // Rejeição: a transação acima já reverteu por completo; o resultado
            // de falha e o marcador commitam juntos em transação própria.
            transactionTemplate.executeWithoutResult(status -> {
                registrarFalha(envelope, causa);
                registrarProcessado(envelope);
            });
        }
    }

    /**
     * Traduz apenas exceções de negócio do comando para {@link RejeicaoDeNegocio}.
     * O registro do marcador fica fora deste método: uma falha dele é
     * infraestrutura (retry/DLQ), nunca rejeição de negócio.
     */
    private void despacharTraduzindoRejeicao(EnvelopeEvento envelope) {
        try {
            despachar(envelope);
        } catch (RuntimeException e) {
            if (ehFalhaDeNegocio(e)) {
                throw new RejeicaoDeNegocio(e);
            }
            throw e;
        }
    }

    /** Rejeição de negócio de um comando, distinta de falha de infraestrutura. */
    private static final class RejeicaoDeNegocio extends RuntimeException {

        private RejeicaoDeNegocio(RuntimeException causa) {
            super(causa.getMessage(), causa);
        }
    }

    private void registrarProcessado(EnvelopeEvento envelope) {
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(envelope.eventId())
                .eventType(envelope.eventType())
                .processedAt(LocalDateTime.now())
                .build());
    }

    private void validar(EnvelopeEvento envelope) {
        if (envelope.eventId() == null || envelope.eventId().isBlank()) {
            throw new MensagemNaoProcessavelException("Envelope sem eventId");
        }
        if (envelope.eventType() == null || envelope.eventType().isBlank()) {
            throw new MensagemNaoProcessavelException("Envelope sem eventType");
        }
        if (envelope.version() == null || !EventosV1.VERSAO.equals(envelope.version())) {
            throw new MensagemNaoProcessavelException("Versão de evento não suportada: " + envelope.version());
        }
        if (envelope.payload() == null || !envelope.payload().isObject()) {
            throw new MensagemNaoProcessavelException("Envelope sem payload de objeto");
        }
        if (!EventosV1.COMANDOS.contains(envelope.eventType())) {
            throw new MensagemNaoProcessavelException("eventType fora do catálogo de comandos v1: " + envelope.eventType());
        }
    }

    private void despachar(EnvelopeEvento envelope) {
        JsonNode payload = envelope.payload();
        switch (envelope.eventType()) {
            case EventosV1.LOTE_CRIAR_SOLICITADO -> planejamentoService.handle(criar(payload));
            case EventosV1.LOTE_EDITAR_SOLICITADO -> planejamentoService.handle(editar(payload));
            case EventosV1.LOTE_CONFIRMAR_SOLICITADO ->
                    planejamentoService.handle(ConfirmarPlanejamentoLote.builder().id(loteId(payload)).build());
            case EventosV1.LOTE_CANCELAR_SOLICITADO ->
                    planejamentoService.handle(CancelarPlanejamentoLote.builder().id(loteId(payload)).build());
            default -> throw new MensagemNaoProcessavelException("Comando não suportado: " + envelope.eventType());
        }
    }

    private CriarPlanejamentoLote criar(JsonNode payload) {
        return CriarPlanejamentoLote.builder()
                .loteId(loteId(payload))
                .rota(RotaId.fromString(texto(payload, "rotaId")))
                .veiculo(VeiculoId.fromString(texto(payload, "veiculoId")))
                .motorista(MotoristaId.fromString(texto(payload, "motoristaId")))
                .dataInicio(dataHora(payload, "dataInicio"))
                .dataFim(dataHora(payload, "dataFim"))
                .diasDaSemana(dias(payload))
                .build();
    }

    private EditarPlanejamentoLote editar(JsonNode payload) {
        return EditarPlanejamentoLote.builder()
                .id(loteId(payload))
                .rota(RotaId.fromString(texto(payload, "rotaId")))
                .veiculo(VeiculoId.fromString(texto(payload, "veiculoId")))
                .motorista(MotoristaId.fromString(texto(payload, "motoristaId")))
                .dataInicio(dataHora(payload, "dataInicio"))
                .dataFim(dataHora(payload, "dataFim"))
                .diasDaSemana(dias(payload))
                .build();
    }

    private PlanejamentoLoteId loteId(JsonNode payload) {
        return PlanejamentoLoteId.fromString(texto(payload, "loteId"));
    }

    private String texto(JsonNode payload, String campo) {
        JsonNode valor = payload.get(campo);
        if (valor == null || valor.isNull() || valor.asText().isBlank()) {
            throw new MensagemNaoProcessavelException("Payload sem campo obrigatório: " + campo);
        }
        return valor.asText();
    }

    private LocalDateTime dataHora(JsonNode payload, String campo) {
        String valor = texto(payload, campo);
        try {
            return LocalDateTime.parse(valor, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new MensagemNaoProcessavelException("Data inválida no campo " + campo + ": " + valor, e);
        }
    }

    private Set<DayOfWeek> dias(JsonNode payload) {
        JsonNode dias = payload.get("diasDaSemana");
        if (dias == null || !dias.isArray() || dias.isEmpty()) {
            throw new MensagemNaoProcessavelException("Payload sem diasDaSemana");
        }
        Set<DayOfWeek> resultado = EnumSet.noneOf(DayOfWeek.class);
        for (JsonNode dia : dias) {
            if (!dia.isInt() || dia.asInt() < 1 || dia.asInt() > 7) {
                throw new MensagemNaoProcessavelException("Dia da semana inválido: " + dia);
            }
            resultado.add(DayOfWeek.of(dia.asInt()));
        }
        return resultado;
    }

    private boolean ehFalhaDeNegocio(RuntimeException e) {
        return e instanceof IllegalArgumentException
                || e instanceof IllegalStateException
                || e instanceof EntityNotFoundException
                || e instanceof ConstraintViolationException
                || e instanceof DataIntegrityViolationException;
    }

    private void registrarFalha(EnvelopeEvento envelope, RuntimeException erro) {
        String loteId = envelope.payload().hasNonNull("loteId")
                ? envelope.payload().get("loteId").asText()
                : null;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loteId", loteId);
        payload.put("comando", envelope.eventType());
        payload.put("motivo", motivo(erro));

        String correlationId = envelope.correlationId() != null
                ? envelope.correlationId()
                : (loteId != null ? loteId : UUID.randomUUID().toString());

        outboxRecorder.registrar(IntegrationEvent.of(
                EventosV1.LOTE_FALHA,
                Instant.now(),
                correlationId,
                tenantProvider.atual().toUUID(),
                payload));
    }

    private String motivo(RuntimeException erro) {
        String motivo = erro.getMessage() != null ? erro.getMessage() : erro.getClass().getSimpleName();
        return motivo.length() > TAMANHO_MAX_MOTIVO ? motivo.substring(0, TAMANHO_MAX_MOTIVO) : motivo;
    }
}
