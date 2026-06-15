-- Alguns IDs embeddables estão sendo serializados com tamanho maior que 36 chars.
ALTER TABLE agendamento.planejamento_lote
    ALTER COLUMN rota TYPE TEXT;

ALTER TABLE agendamento.planejamento_lote
    ALTER COLUMN veiculo TYPE TEXT;

ALTER TABLE agendamento.planejamento_lote
    ALTER COLUMN motorista TYPE TEXT;

