-- Hibernate está persistindo RecorrenciaSemanal.diasDaSemana como smallint[].
ALTER TABLE agendamento.planejamento_lote
    DROP COLUMN IF EXISTS dias_da_semana;

ALTER TABLE agendamento.planejamento_lote
    ADD COLUMN dias_da_semana SMALLINT[] NULL;

