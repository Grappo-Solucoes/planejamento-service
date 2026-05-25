package br.com.busco.planejamento.ui.rest;

import br.com.busco.planejamento.app.PlanejamentoService;
import br.com.busco.planejamento.app.cmd.*;
import br.com.busco.planejamento.domain.PlanejamentoLote;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/planejamentos")
@AllArgsConstructor
public class PlanejamentoController {

    private final PlanejamentoService planejamentoService;

    @PostMapping
    public ResponseEntity<Void> criarPlanejamento(@Valid @RequestBody CriarPlanejamentoLote cmd) {
        PlanejamentoLoteId id = planejamentoService.handle(cmd);

        return ResponseEntity
                .created(URI.create("/api/planejamentos/" + id.toUUID()))
                .build();
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmarPlanejamento(@PathVariable UUID id) {
        ConfirmarPlanejamentoLote cmd = ConfirmarPlanejamentoLote.builder()
                .id(PlanejamentoLoteId.fromString(id.toString()))
                .build();

        PlanejamentoLoteId planejamentoId = planejamentoService.handle(cmd);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarPlanejamento(@PathVariable UUID id) {
        CancelarPlanejamentoLote cmd = CancelarPlanejamentoLote.builder()
                .id(PlanejamentoLoteId.fromString(id.toString()))
                .build();

        PlanejamentoLoteId planejamentoId = planejamentoService.handle(cmd);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/suspender")
    public ResponseEntity<Void> suspenderPlanejamento(@PathVariable UUID id) {
        SuspenderPlanejamentoLote cmd = SuspenderPlanejamentoLote.builder()
                .id(PlanejamentoLoteId.fromString(id.toString()))
                .build();

        PlanejamentoLoteId planejamentoId = planejamentoService.handle(cmd);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Void> buscarPorId(@PathVariable UUID id) {
        PlanejamentoLote planejamento = planejamentoService.buscarPorId(PlanejamentoLoteId.fromString(id.toString()));

        return ResponseEntity.ok().build();
    }
}