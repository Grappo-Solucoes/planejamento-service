package br.com.busco.planejamento.infra.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aceita os JWT HS256 emitidos pelo busco-api no perfil de integração e garante
 * que as probes de Kubernetes (liveness/readiness) são públicas, enquanto o
 * restante do Actuator e os endpoints de negócio seguem autenticados
 * (service-runtime D6/CONTRACT de autenticação).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:segurancadb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS agendamento;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=null",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.jakarta.persistence.schema-generation.database.action=drop-and-create",
        "spring.flyway.enabled=false",
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "spring.rabbitmq.username=guest",
        "spring.rabbitmq.password=guest",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "planejamento.tenant-id=00000000-0000-0000-0000-000000000001",
        "planejamento.outbox.publisher.enabled=false",
        "planejamento.jwt.secret=dGVzdGUtcGxhbmVqYW1lbnRvLXNlcnZpY2Utc2VjcmV0LTMyYg",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DisplayName("Segurança HS256 - Testes de Integração")
class SegurancaHS256IT {

    private static final String SEGREDO_BASE64URL = "dGVzdGUtcGxhbmVqYW1lbnRvLXNlcnZpY2Utc2VjcmV0LTMyYg";
    private static final String TENANT = "00000000-0000-0000-0000-000000000001";
    private static final String ID_INEXISTENTE = "00000000-0000-0000-0000-0000000000ff";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Liveness responde 200 sem token (probe do kubelet)")
    void livenessSemTokenRetorna200() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Readiness responde 200 sem token (probe do kubelet)")
    void readinessSemTokenRetorna200() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint de negócio sem token retorna 401")
    void endpointDeNegocioSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/planejamentos/{id}", ID_INEXISTENTE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Actuator fora de health (ex.: info) sem token retorna 401")
    void actuatorForaDeHealthSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token HS256 válido autoriza (200)")
    void tokenValidoAutoriza() throws Exception {
        mockMvc.perform(get("/actuator/info")
                        .header("Authorization", "Bearer " + token(SEGREDO_BASE64URL)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint /actuator/prometheus registrado responde 200 com token válido e expõe métricas")
    void prometheusComTokenValidoRetorna200() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + token(SEGREDO_BASE64URL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("# HELP")))
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    @Test
    @DisplayName("Assinatura inválida retorna 401")
    void assinaturaInvalidaRetorna401() throws Exception {
        String outroSegredo = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes());

        mockMvc.perform(get("/actuator/info")
                        .header("Authorization", "Bearer " + token(outroSegredo)))
                .andExpect(status().isUnauthorized());
    }

    private String token(String segredoBase64Url) throws Exception {
        byte[] segredo = Base64.getUrlDecoder().decode(segredoBase64Url);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("usuario-teste")
                .claim("tenant_id", TENANT)
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(segredo));
        return jwt.serialize();
    }
}
