package br.com.busco.planejamento.ui.rest;

import br.com.busco.planejamento.infra.repo.PlanejamentoRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PlanejamentoController - paridade de contrato com o monólito")
class PlanejamentoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PlanejamentoRepositoryImpl repository;

    private String payloadCriacao() throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("dataInicio", "2026-11-01");
        payload.put("dataFim", "2026-12-01");
        payload.put("diasSemana", new String[]{"MONDAY", "WEDNESDAY"});
        payload.put("horaViagem", "07:30:00");
        payload.put("rota", UUID.randomUUID().toString());
        payload.put("motorista", UUID.randomUUID().toString());
        payload.put("veiculo", UUID.randomUUID().toString());
        payload.put("monitor", UUID.randomUUID().toString());
        payload.put("alunos", new String[]{UUID.randomUUID().toString()});
        payload.put("calendarios", new String[]{UUID.randomUUID().toString()});
        payload.put("grupoChecklistInicial", UUID.randomUUID().toString());
        payload.put("grupoChecklistFinal", UUID.randomUUID().toString());
        return mapper.writeValueAsString(payload);
    }

    private String criarPlanejamento() throws Exception {
        var antes = repository.findAll().stream().map(p -> p.getId().toUUID()).collect(java.util.stream.Collectors.toSet());
        mvc.perform(post("/api/planejamento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadCriacao()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
        return repository.findAll().stream()
                .map(p -> p.getId().toUUID())
                .filter(id -> !antes.contains(id))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("POST /api/planejamento responde 201 com Location")
    void criarResponde201() throws Exception {
        criarPlanejamento();
    }

    @Test
    @DisplayName("PUT /{id} responde 201; inexistente responde 404")
    void editarResponde201E404() throws Exception {
        String id = criarPlanejamento();

        mvc.perform(put("/api/planejamento/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("horaViagem", "08:00:00"))))
                .andExpect(status().isCreated());

        mvc.perform(put("/api/planejamento/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("horaViagem", "08:00:00"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /{id}/validar responde 201; fora de EM_ANALISE responde erro; inexistente 404")
    void validarResponde201ErroE404() throws Exception {
        String id = criarPlanejamento();

        mvc.perform(patch("/api/planejamento/" + id + "/validar"))
                .andExpect(status().isCreated());

        mvc.perform(patch("/api/planejamento/" + id + "/validar"))
                .andExpect(status().is4xxClientError());

        mvc.perform(patch("/api/planejamento/" + UUID.randomUUID() + "/validar"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /{id}/cancelar responde 201; inexistente 404")
    void cancelarResponde201E404() throws Exception {
        String id = criarPlanejamento();

        mvc.perform(patch("/api/planejamento/" + id + "/cancelar"))
                .andExpect(status().isCreated());

        mvc.perform(patch("/api/planejamento/" + UUID.randomUUID() + "/cancelar"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /{id}/duplicar responde 201; inexistente 404")
    void duplicarResponde201E404() throws Exception {
        String id = criarPlanejamento();

        mvc.perform(patch("/api/planejamento/" + id + "/duplicar"))
                .andExpect(status().isCreated());

        mvc.perform(patch("/api/planejamento/" + UUID.randomUUID() + "/duplicar"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{id} responde 200 com o recurso; inexistente responde 404")
    void buscarResponde200E404() throws Exception {
        String id = criarPlanejamento();

        mvc.perform(get("/api/planejamento/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("EM_ANALISE"))
                .andExpect(jsonPath("$.horaViagem").value("07:30:00"))
                .andExpect(jsonPath("$.diasDaSemana").isArray())
                .andExpect(jsonPath("$.rotaId").isNotEmpty())
                .andExpect(jsonPath("$.passageiros").isArray())
                .andExpect(jsonPath("$.calendarioIds").isArray());

        mvc.perform(get("/api/planejamento/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
