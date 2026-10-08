package br.com.busco.planejamento.infra.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class SecurityConfig {

    @Bean
    @Profile("!test")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Permite acesso ao Swagger UI e API docs sem autenticação
                        .requestMatchers(
                                "/swagger-ui/**",   // Recursos da interface (JS, CSS, HTML)
                                "/v3/api-docs/**",  // Documento JSON da API
                                "/swagger-ui.html" , // Redirecionamento (se usado)
                                "/swagger-resources/**",    // Adicionado
                                "/webjars/**",              // Adicionado
                                "/api-docs/**"              // Adicionado (caso use)
                        ).permitAll()
                        // Qualquer outra requisição precisa de autenticação
                        .anyRequest().authenticated()
                ).oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // API REST sem estado
                );;
        return http.build();
    }

    @Bean
    @Profile("test")
    public SecurityFilterChain noAuthFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
        return http.build();
    }

    /**
     * Aceita os JWT HS256 emitidos pelo busco-api: segredo compartilhado
     * ({@code JWT_SECRET}) decodificado em BASE64URL, como no JwtTokenUtil do
     * busco-api. O issuer OAuth2 continua disponível como opção por
     * configuração (D6).
     */
    @Bean
    @Profile("!test")
    public JwtDecoder jwtDecoder(
            @Value("${planejamento.jwt.secret:}") String segredoBase64Url,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {
        if (segredoBase64Url != null && !segredoBase64Url.isBlank()) {
            byte[] segredo = Base64.getUrlDecoder().decode(segredoBase64Url.trim());
            SecretKey chave = new SecretKeySpec(segredo, "HmacSHA256");
            return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        }
        if (issuerUri != null && !issuerUri.isBlank()) {
            return JwtDecoders.fromIssuerLocation(issuerUri);
        }
        throw new IllegalStateException("Configure planejamento.jwt.secret (HS256/JWT_SECRET) "
                + "ou spring.security.oauth2.resourceserver.jwt.issuer-uri");
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(Arrays.asList("*")); // Em produção, especifique as origens
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
//        configuration.setExposedHeaders(Arrays.asList("Authorization"));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
//        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

//    @Bean
//    public JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
//        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
//        converter.setAuthorityPrefix("ROLE_");
//        converter.setAuthoritiesClaimName("authorities"); // Ajuste conforme seu token
//        return converter;
//    }
}
