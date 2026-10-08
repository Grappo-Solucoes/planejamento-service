package br.com.busco.planejamento.sk.ddd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolve o tenant do processamento atual: claim {@code tenant_id} do token
 * quando presente, senão o tenant fixo da instalação
 * ({@code planejamento.tenant-id}, default do contrato).
 */
@Component
public class TenantProvider {

    private final TenantId tenantPadrao;

    public TenantProvider(@Value("${planejamento.tenant-id}") String tenantPadrao) {
        this.tenantPadrao = TenantId.fromString(tenantPadrao);
    }

    public TenantId atual() {
        TenantId atual = TenantContext.get();
        return atual != null ? atual : tenantPadrao;
    }

    public TenantId tenantPadrao() {
        return tenantPadrao;
    }
}
