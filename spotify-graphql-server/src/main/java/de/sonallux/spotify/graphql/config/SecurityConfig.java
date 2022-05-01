package de.sonallux.spotify.graphql.config;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.FormLoginSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;

@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) throws Exception {
        return http
            .csrf(CsrfSpec::disable)
            .authorizeExchange(c -> c
                .pathMatchers("/account").authenticated()
                .pathMatchers("/**").permitAll()
            )
            .headers(c -> c.frameOptions().mode(Mode.SAMEORIGIN))
            .formLogin(FormLoginSpec::disable)
            .exceptionHandling(c -> c
                // Just return 401 Unauthorized if authentication is required
                .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .oauth2Login(Customizer.withDefaults())
            .build();
    }
}
