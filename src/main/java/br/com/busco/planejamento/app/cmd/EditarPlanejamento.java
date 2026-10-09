package br.com.busco.planejamento.app.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EditarPlanejamento {
    private UUID id;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private LocalTime horaViagem;
    private List<DayOfWeek> diasSemana;
    private UUID rota;
    private List<UUID> calendarios;
    private UUID veiculo;
    private UUID motorista;
    private UUID monitor;
    private List<UUID> alunos;
    private UUID grupoChecklistInicial;
    private UUID grupoChecklistFinal;
}
