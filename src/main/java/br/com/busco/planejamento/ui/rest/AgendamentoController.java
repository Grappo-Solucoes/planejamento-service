package br.com.busco.planejamento.ui.rest;

import br.com.busco.planejamento.app.AgendamentoService;
import br.com.busco.planejamento.app.cmd.*;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/agendamentos")
@AllArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<Void> criarAgendamento(@Valid @RequestBody CriarAgendamento cmd) {
        AgendamentoOperacionalId id = agendamentoService.handle(cmd);

        return ResponseEntity
                .created(URI.create("/api/agendamentos/" + id.toUUID()))
                .build();
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmarAgendamento(@PathVariable UUID id) {
        ConfirmarAgendamento cmd = ConfirmarAgendamento.builder()
                .id(AgendamentoOperacionalId.fromString(id.toString()))
                .build();

        AgendamentoOperacionalId agendamentoId = agendamentoService.handle(cmd);

        return ResponseEntity.ok().build();
//                .ok(URI.create("/api/agendamentos/" + agendamentoId.toUUID()));
    }

    @PostMapping("/{id}/revalidar")
    public ResponseEntity<Void> revalidarAgendamento(@PathVariable UUID id) {
        RevalidarAgendamento cmd = RevalidarAgendamento.builder()
                .id(AgendamentoOperacionalId.fromString(id.toString()))
                .build();

        AgendamentoOperacionalId agendamentoId = agendamentoService.handle(cmd);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarAgendamento(@PathVariable UUID id) {
        CancelarAgendamento cmd = CancelarAgendamento.builder()
                .id(AgendamentoOperacionalId.fromString(id.toString()))
                .build();

        AgendamentoOperacionalId agendamentoId = agendamentoService.handle(cmd);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancelar-lote")
    public ResponseEntity<Void> cancelarAgendamentoPorLote(@PathVariable UUID id) {
        CancelarAgendamentoPorLote cmd = CancelarAgendamentoPorLote.builder()
                .id(AgendamentoOperacionalId.fromString(id.toString()))
                .build();

        AgendamentoOperacionalId agendamentoId = agendamentoService.handle(cmd);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/conflito")
    public ResponseEntity<Void> colocarEmConflito(@PathVariable UUID id) {
        ColocarAgendamentoEmConflito cmd = ColocarAgendamentoEmConflito.builder()
                .id(AgendamentoOperacionalId.fromString(id.toString()))
                .build();

        AgendamentoOperacionalId agendamentoId = agendamentoService.handle(cmd);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .build();
    }
}