package br.com.busco.planejamento;

import br.com.busco.planejamento.infra.job.AgendamentoAutoProcessamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que o serviço sobe no perfil {@code integration} (o perfil do cluster
 * integrado ao busco-api) sem os jobs locais: o auto-processamento de
 * agendamentos fica desabilitado por configuração e não há mais nenhum
 * agendamento de partição órfão.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:integrationdb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS agendamento;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=null",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.jakarta.persistence.schema-generation.database.action=drop-and-create",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "spring.rabbitmq.username=guest",
        "spring.rabbitmq.password=guest",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "planejamento.tenant-id=00000000-0000-0000-0000-000000000001",
        "planejamento.outbox.publisher.enabled=false"
})
@ActiveProfiles("integration")
class PlanejamentoServiceApplicationIntegrationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("Contexto sobe no perfil de integração sem auto-processamento nem partições órfãs")
    void contextoDaAplicacaoSobeNoPerfilDeIntegracaoSemJobs() {
        assertThat(context.getBeanNamesForType(AgendamentoAutoProcessamento.class)).isEmpty();
        assertThat(context.getBeanDefinitionNames())
                .noneMatch(nome -> nome.toLowerCase().contains("partition"));
    }

    @TestConfiguration
    static class JwtDecoderStubConfiguration {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new IllegalStateException("Stub de teste: decodificação não é exercitada neste contexto");
            };
        }
    }
}
