package br.com.busco.planejamento.ui.rest;

import br.com.busco.planejamento.domain.Passageiro;
import br.com.busco.planejamento.domain.Planejamento;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PlanejamentoResponse(
        UUID id,
        String status,
        LocalDate dataInicio,
        LocalDate dataFim,
        List<DayOfWeek> diasDaSemana,
        LocalTime horaViagem,
        UUID rotaId,
        UUID motoristaId,
        UUID veiculoId,
        UUID monitorId,
        List<PassageiroResponse> passageiros,
        List<UUID> calendarioIds,
        UUID grupoChecklistInicialId,
        UUID grupoChecklistFinalId,
        String codigo) {

    public record PassageiroResponse(UUID passageiroId, String nome, String ponto, boolean ativo) {
        static PassageiroResponse of(Passageiro passageiro) {
            return new PassageiroResponse(
                    passageiro.getPassageiroId(),
                    passageiro.getNome(),
                    passageiro.getPonto(),
                    passageiro.isAtivo());
        }
    }

    public static PlanejamentoResponse of(Planejamento planejamento) {
        return new PlanejamentoResponse(
                UUID.fromString(planejamento.getId().toUUID()),
                planejamento.getStatus().name(),
                planejamento.getDataInicio(),
                planejamento.getDataFim(),
                List.copyOf(planejamento.getDiasDaSemana()),
                planejamento.getHoraViagem(),
                planejamento.getRotaId(),
                planejamento.getMotoristaId(),
                planejamento.getVeiculoId(),
                planejamento.getMonitorId(),
                planejamento.getPassageiros().stream().map(PassageiroResponse::of).toList(),
                List.copyOf(planejamento.getCalendarioIds()),
                planejamento.getGrupoChecklistInicialId(),
                planejamento.getGrupoChecklistFinalId(),
                planejamento.getCodigo());
    }
}
