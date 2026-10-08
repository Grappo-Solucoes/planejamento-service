package br.com.busco.planejamento.infra.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova que as migrações Flyway V1–V7 são consistentes com as entidades
 * JPA contra um Postgres real: o contexto sobe com {@code ddl-auto=validate}
 * (application.properties) e as colunas da outbox/inbox são VARCHAR(36),
 * alinhadas a V1 e ao mapeamento {@code String} das entidades.
 */
@SpringBootTest(properties = {
        // url/username/password vêm do container via @ServiceConnection
        "spring.datasource.hikari.data-source-properties.sslmode=disable",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "spring.rabbitmq.username=guest",
        "spring.rabbitmq.password=guest",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "planejamento.tenant-id=00000000-0000-0000-0000-000000000001",
        "planejamento.outbox.publisher.enabled=false",
        "planejamento.jobs.auto-processamento.enabled=false",
        "planejamento.jwt.secret=dGVzdGUtcGxhbmVqYW1lbnRvLXNlcnZpY2Utc2VjcmV0LTMyYg"
})
@Testcontainers
@DisplayName("Migrações Flyway x entidades - Postgres real")
class FlywaySchemaValidationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway aplica V1–V7 sem falha e o validate do Hibernate passa no boot")
    void flywayAplicaTodasAsMigracoesSemFalha() {
        // Se o contexto subiu, o ddl-auto=validate passou contra o schema migrado.
        Integer sucesso = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM agendamento.flyway_schema_history_agendamento
                WHERE success = true AND version IN ('1','2','3','4','5','6','7')
                """, Integer.class);
        assertThat(sucesso).isEqualTo(7);

        Integer falhas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM agendamento.flyway_schema_history_agendamento
                WHERE success = false
                """, Integer.class);
        assertThat(falhas).isZero();
    }

    @Test
    @DisplayName("Colunas da outbox e da inbox são VARCHAR(36), como as entidades String")
    void colunasDaOutboxEInboxSaoVarchar36() {
        List<String> colunas = jdbcTemplate.queryForList("""
                SELECT table_name || '.' || column_name || '=' || data_type
                       || '(' || COALESCE(character_maximum_length, 0) || ')'
                FROM information_schema.columns
                WHERE table_schema = 'agendamento'
                  AND ((table_name = 'outbox_events'
                        AND column_name IN ('id', 'event_id', 'correlation_id', 'tenant_id'))
                    OR (table_name = 'processed_events' AND column_name = 'event_id'))
                ORDER BY table_name, column_name
                """, String.class);

        assertThat(colunas).containsExactly(
                "outbox_events.correlation_id=character varying(36)",
                "outbox_events.event_id=character varying(36)",
                "outbox_events.id=character varying(36)",
                "outbox_events.tenant_id=character varying(36)",
                "processed_events.event_id=character varying(36)");
    }
}
