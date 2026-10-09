-- V8__planejamento_tenant_not_null_e_fks.sql (PostgreSQL + H2 compatível)
-- Pós-review: tenant_id NOT NULL + FKs das tabelas-coleção para o pai.
-- RLS (ENABLE ROW LEVEL SECURITY + policy auth.jwt()) fica como follow-up do
-- change de corte: a sintaxe é exclusiva do Postgres e quebraria os ITs em H2.

ALTER TABLE agendamento.planejamento
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE agendamento.planejamento_dias_da_semana
    ADD CONSTRAINT fk_dias_planejamento
    FOREIGN KEY (planejamento_id) REFERENCES agendamento.planejamento (id);

ALTER TABLE agendamento.planejamento_passageiro
    ADD CONSTRAINT fk_passageiro_planejamento
    FOREIGN KEY (planejamento_id) REFERENCES agendamento.planejamento (id);

ALTER TABLE agendamento.planejamento_calendario
    ADD CONSTRAINT fk_calendario_planejamento
    FOREIGN KEY (planejamento_id) REFERENCES agendamento.planejamento (id);
