package de.sonallux.spotify.graphql.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) throws Exception {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(c -> c
                .pathMatchers("/oauth2/authorize/spotify").permitAll()
                .pathMatchers("/graphql").permitAll()
                .pathMatchers("/graphiql").authenticated()
            )
            .oauth2Login(Customizer.withDefaults())
            .build();
    }
}
