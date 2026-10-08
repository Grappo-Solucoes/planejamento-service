package br.com.busco.planejamento.sk.ddd;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TenantFilter - Testes Unitários")
class TenantFilterTest {

    private static final String TENANT_FIXO = "00000000-0000-0000-0000-000000000001";

    private final TenantFilter filtro = new TenantFilter(new TenantProvider(TENANT_FIXO));

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Usa o claim tenant_id quando presente")
    void deveUsarClaimQuandoPresente() throws Exception {
        String tenant = UUID.randomUUID().toString();
        SecurityContextHolder.getContext().setAuthentication(autenticacaoComClaim(tenant));

        AtomicReference<TenantId> durante = new AtomicReference<>();
        filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> durante.set(TenantContext.get()));

        assertThat(durante.get()).isEqualTo(TenantId.fromString(tenant));
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    @DisplayName("Usa o tenant fixo quando o claim está ausente")
    void deveUsarTenantFixoQuandoClaimAusente() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(autenticacaoComClaim(null));

        AtomicReference<TenantId> durante = new AtomicReference<>();
        filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> durante.set(TenantContext.get()));

        assertThat(durante.get()).isEqualTo(TenantId.fromString(TENANT_FIXO));
        assertThat(TenantContext.get()).isNull();
    }

    private JwtAuthenticationToken autenticacaoComClaim(String tenant) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256").subject("teste");
        if (tenant != null) {
            builder.claim("tenant_id", tenant);
        }
        return new JwtAuthenticationToken(builder.build());
    }
}
