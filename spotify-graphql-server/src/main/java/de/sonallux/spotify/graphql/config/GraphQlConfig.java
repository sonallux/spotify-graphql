package de.sonallux.spotify.graphql.config;

import de.sonallux.spotify.graphql.wiring.SpotifyWiringFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GraphQlConfig {
    @Value("${spotify.base-url}")
    private String spotifyBaseUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.create(spotifyBaseUrl);
    }

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return builder -> builder.wiringFactory(new SpotifyWiringFactory());
    }
}
