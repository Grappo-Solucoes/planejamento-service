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
public class PlanejarViagem {
    private LocalDate dataInicio;
    private LocalTime horaViagem;
    private LocalDate dataFim;
    private UUID rota;
    private List<UUID> calendarios;
    private List<DayOfWeek> diasSemana;
    private UUID motorista;
    private UUID veiculo;
    private UUID monitor;
    private List<UUID> alunos;
    private UUID grupoChecklistInicial;
    private UUID grupoChecklistFinal;
}
