package br.com.busco.planejamento.sk.ddd;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Order(2)
@Component
@RequiredArgsConstructor
public class HibernateTenantFilter extends OncePerRequestFilter {

    private final EntityManagerFactory entityManagerFactory;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        EntityManager entityManager = EntityManagerFactoryUtils
                .getTransactionalEntityManager(entityManagerFactory);

        try {

            var tenant = TenantContext.get();

            if (tenant != null && entityManager != null) {
                entityManager.unwrap(Session.class)
                        .enableFilter("tenantFilter")
                        .setParameter("tenantId", tenant.toUUID());
            }

            filterChain.doFilter(request, response);

        } finally {
            if (entityManager != null) {
                entityManager.unwrap(Session.class)
                        .disableFilter("tenantFilter");
            }
        }
    }
}
