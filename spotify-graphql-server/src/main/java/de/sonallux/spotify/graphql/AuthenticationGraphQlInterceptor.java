package de.sonallux.spotify.graphql;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
public class AuthenticationGraphQlInterceptor implements WebGraphQlInterceptor {
    public static final String SPOTIFY_AUTHORIZATION_CONTEXT_KEY = "spotify_authorization";

    private final ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        return getSpotifyAuthorizationFromOauth2()
            .switchIfEmpty(getSpotifyAuthorizationFromRequest(request))
            .map(this::getSpotifyAuthorizationContext)
            .defaultIfEmpty(Context.empty())
            .flatMap(context -> chain.next(request).contextWrite(context));
    }

    private Mono<String> getSpotifyAuthorizationFromRequest(WebGraphQlRequest request) {
        return Mono.justOrEmpty(request.getHeaders().getFirst(AUTHORIZATION));
    }

    private Mono<String> getSpotifyAuthorizationFromOauth2() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMap(this::getOAuth2AuthorizedClient)
            .map(authorizedClient -> {
                var token = authorizedClient.getAccessToken();
                return token.getTokenType().getValue() + " " + token.getTokenValue();
            });
    }

    private Mono<OAuth2AuthorizedClient> getOAuth2AuthorizedClient(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
            return oAuth2AuthorizedClientService.loadAuthorizedClient(oauth2Auth.getAuthorizedClientRegistrationId(), oauth2Auth.getName());
        }
        return Mono.empty();
    }

    private Context getSpotifyAuthorizationContext(String spotifyAuthorization) {
        return Context.of(SPOTIFY_AUTHORIZATION_CONTEXT_KEY, spotifyAuthorization);
    }
}
