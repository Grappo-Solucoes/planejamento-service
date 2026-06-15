-- Alinha schema com as entidades JPA atuais para ambiente local.

-- agendamento_operacional: campos embeddable CapacidadeVeiculo
ALTER TABLE agendamento.agendamento_operacional
    ADD COLUMN IF NOT EXISTS passageiros_sentados INTEGER;

ALTER TABLE agendamento.agendamento_operacional
    ADD COLUMN IF NOT EXISTS passageiros_em_pe INTEGER;

ALTER TABLE agendamento.agendamento_operacional
    ADD COLUMN IF NOT EXISTS cadeirantes INTEGER;

ALTER TABLE agendamento.agendamento_operacional
    ADD COLUMN IF NOT EXISTS total_especial_necessidades INTEGER;

UPDATE agendamento.agendamento_operacional
SET passageiros_sentados = COALESCE(passageiros_sentados, 0),
    passageiros_em_pe = COALESCE(passageiros_em_pe, 0),
    cadeirantes = COALESCE(cadeirantes, 0),
    total_especial_necessidades = COALESCE(total_especial_necessidades, 0);

-- planejamento_lote: colunas usadas atualmente pelo Hibernate
ALTER TABLE agendamento.planejamento_lote
    ADD COLUMN IF NOT EXISTS rota VARCHAR(36);

ALTER TABLE agendamento.planejamento_lote
    ADD COLUMN IF NOT EXISTS veiculo VARCHAR(36);

ALTER TABLE agendamento.planejamento_lote
    ADD COLUMN IF NOT EXISTS motorista VARCHAR(36);

ALTER TABLE agendamento.planejamento_lote
    ADD COLUMN IF NOT EXISTS inicio TIMESTAMP;

ALTER TABLE agendamento.planejamento_lote
    ADD COLUMN IF NOT EXISTS fim TIMESTAMP;

ALTER TABLE agendamento.planejamento_lote
    ADD COLUMN IF NOT EXISTS dias_da_semana BYTEA;

UPDATE agendamento.planejamento_lote
SET rota = COALESCE(rota, rota_id),
    veiculo = COALESCE(veiculo, veiculo_id),
    motorista = COALESCE(motorista, motorista_id),
    inicio = COALESCE(inicio, periodo_inicio),
    fim = COALESCE(fim, periodo_fim);

-- Nova entidade: AlocacaoPassageiro
CREATE TABLE IF NOT EXISTS agendamento.alocacao_passageiro (
    id VARCHAR(36) PRIMARY KEY,
    agendamento_id VARCHAR(36) NOT NULL,
    passageiro_id VARCHAR(36) NOT NULL,
    ponto_embarque_id VARCHAR(36) NOT NULL,
    ponto_desembarque_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    confirmado_em TIMESTAMP NULL,
    cancelado_em TIMESTAMP NULL,
    cancelado_por VARCHAR(255) NULL,
    tenant_id VARCHAR(36) NOT NULL,
    version BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_alocacao_agendamento_id
    ON agendamento.alocacao_passageiro(agendamento_id);

CREATE INDEX IF NOT EXISTS idx_alocacao_passageiro_id
    ON agendamento.alocacao_passageiro(passageiro_id);

ALTER TABLE agendamento.alocacao_passageiro
    ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "tenant isolation alocacao_passageiro"
    ON agendamento.alocacao_passageiro;

CREATE POLICY "tenant isolation alocacao_passageiro"
    ON agendamento.alocacao_passageiro
    FOR ALL
    USING (
        tenant_id = (auth.jwt() ->> 'tenant_id')
    )
    WITH CHECK (
        tenant_id = (auth.jwt() ->> 'tenant_id')
    );

