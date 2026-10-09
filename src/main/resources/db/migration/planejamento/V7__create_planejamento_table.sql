-- V7__create_planejamento_table.sql (PostgreSQL)
-- Agregado desacoplado: referências externas só por ID, passageiros como VO.
-- Sem FKs cruzadas; só tabelas do próprio schema.

CREATE TABLE IF NOT EXISTS agendamento.planejamento (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(20),
    data_inicio DATE,
    data_fim DATE,
    hora_viagem TIME,
    rota_id UUID,
    motorista_id UUID,
    veiculo_id UUID,
    monitor_id UUID,
    grupo_checklist_inicial_id UUID,
    grupo_checklist_final_id UUID,
    codigo VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_planejamento_status ON agendamento.planejamento(status);
CREATE INDEX IF NOT EXISTS idx_planejamento_rota ON agendamento.planejamento(rota_id);

CREATE TABLE IF NOT EXISTS agendamento.planejamento_dias_da_semana (
    planejamento_id VARCHAR(36) NOT NULL,
    dia_da_semana VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS agendamento.planejamento_passageiro (
    planejamento_id VARCHAR(36) NOT NULL,
    passageiro_id UUID,
    nome VARCHAR(255),
    ponto VARCHAR(255),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_planejamento_passageiro_id
    ON agendamento.planejamento_passageiro(planejamento_id);

CREATE TABLE IF NOT EXISTS agendamento.planejamento_calendario (
    planejamento_id VARCHAR(36) NOT NULL,
    calendario_id UUID NOT NULL
);

CREATE TRIGGER update_planejamento_updated_at
    BEFORE UPDATE ON agendamento.planejamento
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
