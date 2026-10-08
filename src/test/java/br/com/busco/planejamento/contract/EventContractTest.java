package br.com.busco.planejamento.contract;

import br.com.busco.planejamento.domain.events.AgendamentoCanceladoPorEdicao;
import br.com.busco.planejamento.domain.events.AgendamentoCanceladoPorPlanejamento;
import br.com.busco.planejamento.domain.events.AgendamentoCriado;
import br.com.busco.planejamento.domain.events.PlanejamentoLoteCancelado;
import br.com.busco.planejamento.domain.events.PlanejamentoLoteConfirmado;
import br.com.busco.planejamento.domain.events.PlanejamentoLoteCriado;
import br.com.busco.planejamento.domain.events.PlanejamentoLoteEditado;
import br.com.busco.planejamento.infra.events.DomainEventToIntegrationEventMapper;
import br.com.busco.planejamento.infra.events.EnvelopeEvento;
import br.com.busco.planejamento.infra.events.EventoJson;
import br.com.busco.planejamento.infra.events.EventosV1;
import br.com.busco.planejamento.infra.events.IntegrationEvent;
import br.com.busco.planejamento.infra.outbox.OutboxEvent;
import br.com.busco.planejamento.sk.ddd.TenantProvider;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de contrato do planejamento-service contra os JSON Schemas v1
 * vendorados em {@code docs/events/v1} (CONTRACT.md, tarefa 2.3).
 *
 * <p>Valida os exemplos do catálogo e as fixtures produzidas pelo próprio
 * serviço (envelope + payloads dos eventos consumidos e publicados), incluindo
 * a checagem de versão {@code 1.0} e do catálogo.</p>
 */
class EventContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path SCHEMAS = Path.of("docs", "events", "v1", "schemas");
    private static final Path EXEMPLOS = Path.of("docs", "events", "v1", "examples");
    private static final JsonSchemaFactory FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private static final String TENANT = "00000000-0000-0000-0000-000000000001";
    private static final PlanejamentoLoteId LOTE_ID =
            PlanejamentoLoteId.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final RotaId ROTA_ID =
            RotaId.fromString("0d1f7a44-2b1c-4c0e-8f6a-9b3e5d7c1a20");
    private static final VeiculoId VEICULO_ID =
            VeiculoId.fromString("3a5b7c9d-1e2f-4a6b-8c0d-2e4f6a8b0c1d");
    private static final MotoristaId MOTORISTA_ID =
            MotoristaId.fromString("9f8e7d6c-5b4a-4392-8170-6f5e4d3c2b1a");
    private static final AgendamentoOperacionalId AGENDAMENTO_ID =
            AgendamentoOperacionalId.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final LocalDateTime FIM = LocalDateTime.of(2026, 10, 24, 8, 0);
    private static final LocalDateTime DATA_AGENDAMENTO = LocalDateTime.of(2026, 10, 12, 8, 0);
    private static final Set<DayOfWeek> DIAS =
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    private static final Instant OCORRIDO_EM = Instant.parse("2026-10-08T12:00:00Z");

    private final EventoJson eventoJson = new EventoJson();
    private final DomainEventToIntegrationEventMapper mapper =
            new DomainEventToIntegrationEventMapper(new TenantProvider(TENANT));

    @Test
    void todosOsExemplosDoCatalogoDevemSerValidos() throws IOException {
        List<Path> exemplos = listarExemplos();
        assertFalse(exemplos.isEmpty(), "nenhum exemplo encontrado em " + EXEMPLOS.toAbsolutePath());

        List<String> erros = new ArrayList<>();
        for (Path exemplo : exemplos) {
            JsonNode json = MAPPER.readTree(exemplo.toFile());
            JsonSchema schema = schemaDe(json.path("eventType").asText());
            Set<ValidationMessage> mensagens = schema.validate(json);
            if (!mensagens.isEmpty()) {
                erros.add(exemplo.getFileName() + " -> " + mensagens);
            }
        }

        assertTrue(erros.isEmpty(), () -> "exemplos inválidos:\n" + String.join("\n", erros));
    }

    @Test
    void deveExistirSchemaEExemploParaCadaEventoDoCatalogo() throws IOException {
        JsonNode defs = MAPPER.readTree(SCHEMAS.resolve("event-defs.schema.json").toFile());
        List<String> catalogo = new ArrayList<>();
        defs.at("/$defs/eventType/enum").forEach(tipo -> catalogo.add(tipo.asText()));

        assertEquals(11, catalogo.size(), "catálogo v1 deve ter 11 eventTypes");
        assertEquals(Set.copyOf(catalogo), EventosV1.CATALOGO,
                "catálogo do serviço deve ser igual ao do contrato v1");
        assertEquals("1.0", EventosV1.VERSAO, "versão do serviço deve ser 1.0");

        for (String eventType : catalogo) {
            assertTrue(Files.exists(SCHEMAS.resolve(eventType + ".schema.json")),
                    "schema ausente para " + eventType);
            assertTrue(Files.exists(EXEMPLOS.resolve(eventType + ".json")),
                    "exemplo ausente para " + eventType);
        }
    }

    @Test
    void exemploSemCampoObrigatorioDoEnvelopeDeveFalhar() throws IOException {
        for (Path exemplo : listarExemplos()) {
            JsonNode json = MAPPER.readTree(exemplo.toFile());
            JsonSchema schema = schemaDe(json.path("eventType").asText());

            for (String campo : List.of("eventId", "eventType", "version", "occurredOn", "correlationId", "tenantId", "payload")) {
                ObjectNode copia = (ObjectNode) json.deepCopy();
                copia.remove(campo);
                assertInvalido(schema, copia, exemplo.getFileName() + " sem " + campo);
            }
        }
    }

    @Test
    void exemploSemCampoObrigatorioDoPayloadDeveFalhar() throws IOException {
        for (Path exemplo : listarExemplos()) {
            JsonNode json = MAPPER.readTree(exemplo.toFile());
            String eventType = json.path("eventType").asText();
            JsonSchema schema = schemaDe(eventType);
            JsonNode schemaJson = MAPPER.readTree(arquivoSchema(eventType).toFile());

            List<String> obrigatorios = new ArrayList<>();
            schemaJson.at("/properties/payload/required").forEach(campo -> obrigatorios.add(campo.asText()));
            assertFalse(obrigatorios.isEmpty(), "schema de " + eventType + " não declara payload.required");

            for (String campo : obrigatorios) {
                ObjectNode copia = (ObjectNode) json.deepCopy();
                ((ObjectNode) copia.path("payload")).remove(campo);
                assertInvalido(schema, copia, eventType + " sem payload." + campo);
            }
        }
    }

    @Test
    void fixturesPublicadasPeloServicoDevemSerValidas() throws IOException {
        Map<String, IntegrationEvent> fixtures = new LinkedHashMap<>();

        fixtures.put(DomainEventToIntegrationEventMapper.TIPO_LOTE_CRIADO,
                mapper.mapear(PlanejamentoLoteCriado.builder()
                        .id(LOTE_ID).rota(ROTA_ID).veiculo(VEICULO_ID).motorista(MOTORISTA_ID)
                        .dataInicio(INICIO).dataFim(FIM).diasDaSemana(DIAS)
                        .occurredOn(OCORRIDO_EM).build()).orElseThrow());
        fixtures.put(DomainEventToIntegrationEventMapper.TIPO_LOTE_EDITADO,
                mapper.mapear(PlanejamentoLoteEditado.builder()
                        .id(LOTE_ID).rota(ROTA_ID).veiculo(VEICULO_ID).motorista(MOTORISTA_ID)
                        .dataInicio(INICIO).dataFim(FIM).diasDaSemana(DIAS)
                        .occurredOn(OCORRIDO_EM).build()).orElseThrow());
        fixtures.put(DomainEventToIntegrationEventMapper.TIPO_LOTE_CONFIRMADO,
                mapper.mapear(PlanejamentoLoteConfirmado.builder()
                        .id(LOTE_ID).quantidadeAgendamentos(3)
                        .occurredOn(OCORRIDO_EM).build()).orElseThrow());
        fixtures.put(DomainEventToIntegrationEventMapper.TIPO_LOTE_CANCELADO,
                mapper.mapear(PlanejamentoLoteCancelado.builder()
                        .id(LOTE_ID).quantidadeAgendamentosCancelados(2)
                        .occurredOn(OCORRIDO_EM).build()).orElseThrow());
        fixtures.put(DomainEventToIntegrationEventMapper.TIPO_AGENDAMENTO_CRIADO,
                mapper.mapear(AgendamentoCriado.builder()
                        .id(AGENDAMENTO_ID).loteId(LOTE_ID).data(DATA_AGENDAMENTO)
                        .rota(ROTA_ID).veiculo(VEICULO_ID).motorista(MOTORISTA_ID)
                        .occurredOn(OCORRIDO_EM).build()).orElseThrow());
        fixtures.put(DomainEventToIntegrationEventMapper.TIPO_AGENDAMENTO_CANCELADO + ".planejamento",
                mapper.mapear(AgendamentoCanceladoPorPlanejamento.builder()
                        .id(AGENDAMENTO_ID).loteId(LOTE_ID).data(DATA_AGENDAMENTO)
                        .occurredOn(OCORRIDO_EM).build()).orElseThrow());
        fixtures.put(DomainEventToIntegrationEventMapper.TIPO_AGENDAMENTO_CANCELADO + ".edicao",
                mapper.mapear(AgendamentoCanceladoPorEdicao.builder()
                        .id(AGENDAMENTO_ID).loteId(LOTE_ID).data(DATA_AGENDAMENTO)
                        .occurredOn(OCORRIDO_EM).build()).orElseThrow());
        fixtures.put(EventosV1.LOTE_FALHA, IntegrationEvent.of(
                EventosV1.LOTE_FALHA, OCORRIDO_EM, LOTE_ID.toUUID(), TENANT,
                Map.of("loteId", LOTE_ID.toUUID(),
                        "comando", EventosV1.LOTE_CONFIRMAR_SOLICITADO,
                        "motivo", "Planejamento não encontrado")));

        for (Map.Entry<String, IntegrationEvent> fixture : fixtures.entrySet()) {
            JsonNode json = json(fixture.getValue());
            String eventType = json.path("eventType").asText();
            assertEquals(fixture.getValue().eventType(), eventType);
            assertTrue(EventosV1.CATALOGO.contains(eventType), "fora do catálogo: " + eventType);
            assertEquals("1.0", json.path("version").asText(), "versão deve ser 1.0");
            assertEquals(LOTE_ID.toUUID(), json.path("correlationId").asText(),
                    "correlationId deve ser o loteId");
            assertEquals(TENANT, json.path("tenantId").asText());
            assertTrue(json.path("occurredOn").asText().endsWith("Z"),
                    () -> "occurredOn deve ser UTC com Z: " + json.path("occurredOn").asText());
            Set<ValidationMessage> mensagens = schemaDe(eventType).validate(json);
            assertTrue(mensagens.isEmpty(),
                    () -> "fixture inválida para " + eventType + ": " + mensagens);
        }

        JsonNode criado = json(fixtures.get(DomainEventToIntegrationEventMapper.TIPO_AGENDAMENTO_CRIADO));
        assertEquals(AGENDAMENTO_ID.toUUID(), criado.at("/payload/agendamentoId").asText());
        assertEquals(LOTE_ID.toUUID(), criado.at("/payload/loteId").asText());
        assertEquals("2026-10-12T08:00:00", criado.at("/payload/data").asText());
        assertEquals(ROTA_ID.toUUID(), criado.at("/payload/rotaId").asText());
        assertEquals(VEICULO_ID.toUUID(), criado.at("/payload/veiculoId").asText());
        assertEquals(MOTORISTA_ID.toUUID(), criado.at("/payload/motoristaId").asText());

        JsonNode cancelado = json(fixtures.get(DomainEventToIntegrationEventMapper.TIPO_AGENDAMENTO_CANCELADO + ".edicao"));
        assertEquals(DomainEventToIntegrationEventMapper.MOTIVO_EDICAO, cancelado.at("/payload/motivo").asText());
    }

    @Test
    void fixturesDosComandosConsumidosDevemSerValidas() throws IOException {
        Map<String, Map<String, Object>> payloads = new LinkedHashMap<>();

        Map<String, Object> agenda = new LinkedHashMap<>();
        agenda.put("loteId", LOTE_ID.toUUID());
        agenda.put("rotaId", ROTA_ID.toUUID());
        agenda.put("veiculoId", VEICULO_ID.toUUID());
        agenda.put("motoristaId", MOTORISTA_ID.toUUID());
        agenda.put("dataInicio", INICIO);
        agenda.put("dataFim", FIM);
        agenda.put("diasDaSemana", List.of(1, 3, 5));

        payloads.put(EventosV1.LOTE_CRIAR_SOLICITADO, agenda);
        payloads.put(EventosV1.LOTE_EDITAR_SOLICITADO, new LinkedHashMap<>(agenda));
        payloads.put(EventosV1.LOTE_CONFIRMAR_SOLICITADO, Map.of("loteId", LOTE_ID.toUUID()));
        payloads.put(EventosV1.LOTE_CANCELAR_SOLICITADO, Map.of("loteId", LOTE_ID.toUUID()));

        for (Map.Entry<String, Map<String, Object>> comando : payloads.entrySet()) {
            IntegrationEvent evento = IntegrationEvent.of(
                    comando.getKey(), OCORRIDO_EM, LOTE_ID.toUUID(), TENANT, comando.getValue());
            JsonNode json = json(evento);

            assertEquals(comando.getKey(), json.path("eventType").asText());
            assertEquals("1.0", json.path("version").asText());
            Set<ValidationMessage> mensagens = schemaDe(comando.getKey()).validate(json);
            assertTrue(mensagens.isEmpty(),
                    () -> "comando inválido " + comando.getKey() + ": " + mensagens);

            // O serviço precisa conseguir ler exatamente o formato do contrato.
            EnvelopeEvento envelope = eventoJson.deJson(envelopeJson(evento), EnvelopeEvento.class);
            assertEquals(comando.getKey(), envelope.eventType());
            assertEquals("1.0", envelope.version());
            assertEquals(LOTE_ID.toUUID(), envelope.payload().path("loteId").asText());
        }
    }

    @Test
    void divergenciaDeEventTypeOuCampoObrigatorioQuebraOTeste() throws IOException {
        IntegrationEvent confirmado = mapper.mapear(PlanejamentoLoteConfirmado.builder()
                .id(LOTE_ID).quantidadeAgendamentos(3)
                .occurredOn(OCORRIDO_EM).build()).orElseThrow();
        JsonNode json = json(confirmado);
        assertTrue(schemaDe(confirmado.eventType()).validate(json).isEmpty());

        ObjectNode tipoDesconhecido = (ObjectNode) json.deepCopy();
        tipoDesconhecido.put("eventType", "planejamento.lote.desconhecido");
        assertFalse(schemaDe(confirmado.eventType()).validate(tipoDesconhecido).isEmpty(),
                "eventType divergente deve invalidar o contrato");
        assertFalse(EventosV1.CATALOGO.contains("planejamento.lote.desconhecido"));

        ObjectNode semCampo = (ObjectNode) json.deepCopy();
        ((ObjectNode) semCampo.path("payload")).remove("quantidadeAgendamentos");
        assertFalse(schemaDe(confirmado.eventType()).validate(semCampo).isEmpty(),
                "remover campo obrigatório do payload deve invalidar o contrato");
    }

    private void assertInvalido(JsonSchema schema, JsonNode json, String cenario) {
        Set<ValidationMessage> mensagens = schema.validate(json);
        assertFalse(mensagens.isEmpty(), () -> "esperava inválido: " + cenario);
    }

    private List<Path> listarExemplos() throws IOException {
        try (Stream<Path> arquivos = Files.list(EXEMPLOS)) {
            return arquivos.filter(arquivo -> arquivo.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private JsonSchema schemaDe(String eventType) {
        return FACTORY.getSchema(arquivoSchema(eventType).toAbsolutePath().toUri());
    }

    private Path arquivoSchema(String eventType) {
        return SCHEMAS.resolve(eventType + ".schema.json");
    }

    private JsonNode json(IntegrationEvent evento) throws IOException {
        return MAPPER.readTree(envelopeJson(evento));
    }

    private String envelopeJson(IntegrationEvent evento) {
        return eventoJson.envelope(OutboxEvent.builder()
                .eventId(evento.eventId())
                .eventType(evento.eventType())
                .version(evento.version())
                .occurredOn(LocalDateTime.ofInstant(evento.occurredOn(), ZoneOffset.UTC))
                .correlationId(evento.correlationId())
                .tenantId(evento.tenantId())
                .payload(eventoJson.paraJson(evento.payload()))
                .build());
    }
}
