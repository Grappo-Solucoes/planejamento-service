package br.com.busco.planejamento.sk.ddd;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Order(1)
@Component
public class TenantFilter extends OncePerRequestFilter {

    private final TenantProvider tenantProvider;

    public TenantFilter(TenantProvider tenantProvider) {
        this.tenantProvider = tenantProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        String tenant = null;
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            tenant = jwtAuth.getToken().getClaimAsString("tenant_id");
        }

        // Claim quando presente; tenant fixo da instalação como fallback (D6).
        TenantContext.set(tenant != null && !tenant.isBlank()
                ? TenantId.fromString(tenant)
                : tenantProvider.tenantPadrao());

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}