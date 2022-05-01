package de.sonallux.spotify.graphql.config;

import de.sonallux.spotify.graphql.security.AuthorizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

@Configuration
public class WebConfig {
    @Bean
    public RouterFunction<ServerResponse> indexRouter(@Value("classpath:/static/index.html") Resource indexHtml) {
        return route(GET("/"), request -> ok().contentType(MediaType.TEXT_HTML).bodyValue(indexHtml));
    }

    @Bean
    public RouterFunction<ServerResponse> account(AuthorizationService authorizationService) {
        return route(GET("/account"), request ->
            authorizationService.getCurrentOAuth2AuthorizedClient()
                .map(authorizedClient -> Map.of(
                    "name", authorizedClient.getPrincipalName(),
                    "type", authorizedClient.getClientRegistration().getClientName()))
                .flatMap(body -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .switchIfEmpty(status(HttpStatus.UNAUTHORIZED).build())
        );
    }
}
