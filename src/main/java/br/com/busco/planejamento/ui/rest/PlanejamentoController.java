package br.com.busco.planejamento.ui.rest;

import br.com.busco.planejamento.app.PlanejamentoService;
import br.com.busco.planejamento.app.cmd.CancelarPlanejamento;
import br.com.busco.planejamento.app.cmd.DuplicarPlanejamento;
import br.com.busco.planejamento.app.cmd.EditarPlanejamento;
import br.com.busco.planejamento.app.cmd.PlanejarViagem;
import br.com.busco.planejamento.app.cmd.ValidarPlanejamento;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

/**
 * Escritas do planejamento (paridade com
 * {@code br.com.grappo.bus.core.planejamento.ui.rest.PlanejamentoController}:
 * mesmos paths, verbos, payloads e status).
 */
@RestController
@RequestMapping("/api/planejamento")
@AllArgsConstructor
public class PlanejamentoController {

    private final PlanejamentoService service;

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid @NonNull PlanejarViagem cmd) {
        service.handle(cmd);

        return ResponseEntity.created(fromCurrentRequest().build().toUri()).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editar(@PathVariable UUID id,
                                       @RequestBody @Valid @NonNull EditarPlanejamento cmd) {
        cmd.setId(id);
        service.handle(cmd);

        return ResponseEntity.created(fromCurrentRequest()
                .path("/").path(id.toString()).build().toUri())
                .build();
    }

    @PatchMapping("/{id}/validar")
    public ResponseEntity<Void> validar(@PathVariable UUID id) {
        ValidarPlanejamento cmd = ValidarPlanejamento.builder().id(id).build();
        service.handle(cmd);

        return ResponseEntity.created(fromCurrentRequest()
                        .path("/").path(id.toString()).build().toUri())
                .build();
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        CancelarPlanejamento cmd = CancelarPlanejamento.builder().id(id).build();
        service.handle(cmd);

        return ResponseEntity.created(fromCurrentRequest()
                        .path("/").path(id.toString()).build().toUri())
                .build();
    }

    @PatchMapping("/{id}/duplicar")
    public ResponseEntity<Void> duplicar(@PathVariable UUID id) {
        DuplicarPlanejamento cmd = DuplicarPlanejamento.builder().id(id).build();
        service.handle(cmd);

        return ResponseEntity.created(fromCurrentRequest()
                        .path("/").path(id.toString()).build().toUri())
                .build();
    }
}
