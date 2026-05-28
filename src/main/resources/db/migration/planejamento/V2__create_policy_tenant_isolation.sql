CREATE SCHEMA IF NOT EXISTS auth;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'auth'
          AND p.proname = 'jwt'
          AND p.pronargs = 0
    ) THEN
        CREATE FUNCTION auth.jwt()
        RETURNS jsonb
        LANGUAGE sql
        STABLE
        AS $function$
            SELECT jsonb_build_object(
                'tenant_id',
                COALESCE(
                    NULLIF(current_setting('app.tenant_id', true), ''),
                    '00000000-0000-0000-0000-000000000001'
                )
            )
        $function$;
    END IF;
END
$$;

ALTER TABLE agendamento.agendamento_operacional ENABLE ROW LEVEL SECURITY;
create policy "tenant isolation agendamento_operacional"
on agendamento.agendamento_operacional
for all
using (
  tenant_id = (auth.jwt() ->> 'tenant_id')
)
with check (
  tenant_id = (auth.jwt() ->> 'tenant_id')
);

ALTER TABLE agendamento.planejamento_lote ENABLE ROW LEVEL SECURITY;
create policy "tenant isolation planejamento_lote"
on agendamento.planejamento_lote
for all
using (
  tenant_id = (auth.jwt() ->> 'tenant_id')
)
with check (
  tenant_id = (auth.jwt() ->> 'tenant_id')
);
