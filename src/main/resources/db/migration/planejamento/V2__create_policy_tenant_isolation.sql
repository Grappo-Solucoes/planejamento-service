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