-- Colunas legadas não são mais preenchidas pelo mapeamento atual.
ALTER TABLE agendamento.planejamento_lote
    ALTER COLUMN periodo_inicio DROP NOT NULL;

ALTER TABLE agendamento.planejamento_lote
    ALTER COLUMN periodo_fim DROP NOT NULL;

ALTER TABLE agendamento.planejamento_lote
    ALTER COLUMN recorrencia_dias DROP NOT NULL;

ALTER TABLE agendamento.planejamento_lote
    ALTER COLUMN rota_id DROP NOT NULL;

ALTER TABLE agendamento.planejamento_lote
    ALTER COLUMN veiculo_id DROP NOT NULL;

ALTER TABLE agendamento.planejamento_lote
    ALTER COLUMN motorista_id DROP NOT NULL;

