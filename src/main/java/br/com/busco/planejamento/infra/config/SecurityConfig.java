package br.com.busco.planejamento.infra.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

//  curl -v http://localhost:8080/api/planejamentos   @Bean
//     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//         http
//                 .authorizeHttpRequests(authorize -> authorize
//                         // Permite acesso ao Swagger UI e API docs sem autenticação
//                         .requestMatchers(
//                                 "/swagger-ui/**",   // Recursos da interface (JS, CSS, HTML)
//                                 "/v3/api-docs/**",  // Documento JSON da API
//                                 "/swagger-ui.html" , // Redirecionamento (se usado)
//                                 "/swagger-resources/**",    // Adicionado
//                                 "/webjars/**",              // Adicionado
//                                 "/api-docs/**"              // Adicionado (caso use)
//                         ).permitAll()
//                         // Qualquer outra requisição precisa de autenticação
//                         .anyRequest().authenticated()
//                 ).oauth2ResourceServer(oauth2 -> oauth2
//                         .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
//                 )
//                 .sessionManagement(session -> session
//                         .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // API REST sem estado
//                 );;
//         return http.build();
//     }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                ).oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
        return http.build();
    }
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        return converter;
    }
}
