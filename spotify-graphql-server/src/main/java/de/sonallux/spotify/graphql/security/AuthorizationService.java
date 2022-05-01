package de.sonallux.spotify.graphql.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    public Mono<OAuth2AuthorizedClient> getCurrentOAuth2AuthorizedClient() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMap(this::getOAuth2AuthorizedClient);
    }

    private Mono<OAuth2AuthorizedClient> getOAuth2AuthorizedClient(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
            return oAuth2AuthorizedClientService.loadAuthorizedClient(oauth2Auth.getAuthorizedClientRegistrationId(), oauth2Auth.getName());
        }
        return Mono.empty();
    }
}
