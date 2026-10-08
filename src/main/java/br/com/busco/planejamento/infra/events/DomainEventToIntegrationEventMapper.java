package br.com.busco.planejamento.infra.events;

import br.com.busco.planejamento.domain.events.*;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import br.com.busco.planejamento.sk.ddd.TenantProvider;
import br.com.busco.planejamento.sk.ids.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Mapeia eventos de domínio do serviço para os eventos de integração do
 * catálogo v1 (CONTRACT §4.2). Eventos de domínio sem representação no
 * contrato são ignorados (ex.: agendamento avulso).
 */
@Component
@RequiredArgsConstructor
public class DomainEventToIntegrationEventMapper {

    public static final String TIPO_LOTE_CRIADO = "planejamento.lote.criado";
    public static final String TIPO_LOTE_EDITADO = "planejamento.lote.editado";
    public static final String TIPO_LOTE_CONFIRMADO = "planejamento.lote.confirmado";
    public static final String TIPO_LOTE_CANCELADO = "planejamento.lote.cancelado";
    public static final String TIPO_AGENDAMENTO_CRIADO = "agendamento.criado";
    public static final String TIPO_AGENDAMENTO_CANCELADO = "agendamento.cancelado";

    public static final String MOTIVO_EDICAO = "Cancelado por edição";
    public static final String MOTIVO_PLANEJAMENTO = "Cancelado por planejamento";

    private final TenantProvider tenantProvider;

    public Optional<IntegrationEvent> mapear(DomainEvent evento) {
        String tenantId = tenantProvider.atual().toUUID();

        if (evento instanceof PlanejamentoLoteCriado e) {
            Map<String, Object> payload = payloadLote(
                    e.getId(), e.getRota(), e.getVeiculo(), e.getMotorista(),
                    e.getDataInicio(), e.getDataFim(), e.getDiasDaSemana());
            payload.put("status", "RASCUNHO");
            return Optional.of(IntegrationEvent.of(
                    TIPO_LOTE_CRIADO, e.getOccurredOn(), e.getId().toUUID(), tenantId, payload));
        }

        if (evento instanceof PlanejamentoLoteEditado e) {
            Map<String, Object> payload = payloadLote(
                    e.getId(), e.getRota(), e.getVeiculo(), e.getMotorista(),
                    e.getDataInicio(), e.getDataFim(), e.getDiasDaSemana());
            return Optional.of(IntegrationEvent.of(
                    TIPO_LOTE_EDITADO, e.getOccurredOn(), e.getId().toUUID(), tenantId, payload));
        }

        if (evento instanceof PlanejamentoLoteConfirmado e) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("loteId", e.getId().toUUID());
            payload.put("quantidadeAgendamentos", e.getQuantidadeAgendamentos());
            return Optional.of(IntegrationEvent.of(
                    TIPO_LOTE_CONFIRMADO, e.getOccurredOn(), e.getId().toUUID(), tenantId, payload));
        }

        if (evento instanceof PlanejamentoLoteCancelado e) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("loteId", e.getId().toUUID());
            payload.put("quantidadeAgendamentosCancelados", e.getQuantidadeAgendamentosCancelados());
            return Optional.of(IntegrationEvent.of(
                    TIPO_LOTE_CANCELADO, e.getOccurredOn(), e.getId().toUUID(), tenantId, payload));
        }

        if (evento instanceof AgendamentoCriado e) {
            if (e.getLoteId() == null) {
                return Optional.empty();
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("agendamentoId", e.getId().toUUID());
            payload.put("loteId", e.getLoteId().toUUID());
            payload.put("data", e.getData());
            payload.put("rotaId", uuid(e.getRota()));
            payload.put("veiculoId", uuid(e.getVeiculo()));
            payload.put("motoristaId", uuid(e.getMotorista()));
            return Optional.of(IntegrationEvent.of(
                    TIPO_AGENDAMENTO_CRIADO, e.getOccurredOn(), e.getLoteId().toUUID(), tenantId, payload));
        }

        if (evento instanceof AgendamentoCanceladoPorPlanejamento e) {
            return mapearCancelamento(e.getId(), e.getLoteId(), e.getData(), MOTIVO_PLANEJAMENTO, e.getOccurredOn(), tenantId);
        }

        if (evento instanceof AgendamentoCanceladoPorEdicao e) {
            return mapearCancelamento(e.getId(), e.getLoteId(), e.getData(), MOTIVO_EDICAO, e.getOccurredOn(), tenantId);
        }

        return Optional.empty();
    }

    private Optional<IntegrationEvent> mapearCancelamento(
            AgendamentoOperacionalId agendamentoId,
            PlanejamentoLoteId loteId,
            LocalDateTime data,
            String motivo,
            java.time.Instant occurredOn,
            String tenantId
    ) {
        if (loteId == null) {
            return Optional.empty();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agendamentoId", agendamentoId.toUUID());
        payload.put("loteId", loteId.toUUID());
        payload.put("data", data);
        payload.put("motivo", motivo);
        return Optional.of(IntegrationEvent.of(
                TIPO_AGENDAMENTO_CANCELADO, occurredOn, loteId.toUUID(), tenantId, payload));
    }

    private Map<String, Object> payloadLote(
            PlanejamentoLoteId loteId,
            RotaId rota,
            VeiculoId veiculo,
            MotoristaId motorista,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            Set<DayOfWeek> diasDaSemana
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loteId", loteId.toUUID());
        payload.put("rotaId", uuid(rota));
        payload.put("veiculoId", uuid(veiculo));
        payload.put("motoristaId", uuid(motorista));
        payload.put("dataInicio", dataInicio);
        payload.put("dataFim", dataFim);
        payload.put("diasDaSemana", diasIso(diasDaSemana));
        return payload;
    }

    private List<Integer> diasIso(Set<DayOfWeek> dias) {
        if (dias == null) {
            return List.of();
        }
        List<Integer> valores = new ArrayList<>(dias.stream().map(DayOfWeek::getValue).toList());
        Collections.sort(valores);
        return valores;
    }

    private String uuid(DomainObjectId id) {
        return id == null ? null : id.toUUID();
    }
}
