-- V1__create_agendamento_operacional_table.sql (PostgreSQL)

-- Tabela de AgendamentoOperacional
CREATE TABLE agendamento_operacional (
                                         id VARCHAR(36) NOT NULL PRIMARY KEY,
                                         rota_id VARCHAR(36) NOT NULL,
                                         veiculo_id VARCHAR(36) NOT NULL,
                                         motorista_id VARCHAR(36) NOT NULL,
                                         data TIMESTAMP NOT NULL,
                                         origem_tipo VARCHAR(10) NOT NULL,
                                         planejamento_id VARCHAR(36),
                                         status VARCHAR(20) NOT NULL,
                                         version BIGINT,
                                         tenant_id varchar(36) not null,
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agendamento_rota ON agendamento_operacional(rota_id);
CREATE INDEX idx_agendamento_veiculo ON agendamento_operacional(veiculo_id);
CREATE INDEX idx_agendamento_motorista ON agendamento_operacional(motorista_id);
CREATE INDEX idx_agendamento_data ON agendamento_operacional(data);
CREATE INDEX idx_agendamento_status ON agendamento_operacional(status);
CREATE INDEX idx_agendamento_origem ON agendamento_operacional(origem_tipo, planejamento_id);

-- Tabela de PlanejamentoLote
CREATE TABLE planejamento_lote (
                                   id VARCHAR(36) NOT NULL PRIMARY KEY,
                                   status VARCHAR(20) NOT NULL,
                                   periodo_inicio TIMESTAMP NOT NULL,
                                   periodo_fim TIMESTAMP NOT NULL,
                                   recorrencia_dias TEXT NOT NULL,
                                   rota_id VARCHAR(36) NOT NULL,
                                   veiculo_id VARCHAR(36) NOT NULL,
                                   motorista_id VARCHAR(36) NOT NULL,
                                   version BIGINT,
                                   tenant_id varchar(36) not null,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_planejamento_status ON planejamento_lote(status);
CREATE INDEX idx_planejamento_rota ON planejamento_lote(rota_id);
CREATE INDEX idx_planejamento_veiculo ON planejamento_lote(veiculo_id);
CREATE INDEX idx_planejamento_motorista ON planejamento_lote(motorista_id);
CREATE INDEX idx_planejamento_periodo ON planejamento_lote(periodo_inicio, periodo_fim);

-- Função para atualizar o updated_at automaticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para agendamento_operacional
CREATE TRIGGER update_agendamento_operacional_updated_at
    BEFORE UPDATE ON agendamento_operacional
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger para planejamento_lote
CREATE TRIGGER update_planejamento_lote_updated_at
    BEFORE UPDATE ON planejamento_lote
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();