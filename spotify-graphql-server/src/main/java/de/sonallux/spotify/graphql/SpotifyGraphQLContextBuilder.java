package de.sonallux.spotify.graphql;

import de.sonallux.spotify.graphql.schema.SpotifyDataLoaderRegistryFactory;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContext;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContextBuilder;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Primary
@AllArgsConstructor
public class SpotifyGraphQLContextBuilder extends DefaultGraphQLServletContextBuilder {

    private final SpotifyDataLoaderRegistryFactory spotifyDataLoaderRegistryFactory;
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Override
    public GraphQLContext build(HttpServletRequest request, HttpServletResponse response) {
        return DefaultGraphQLServletContext.createServletContext()
            .with(request)
            .with(request)
            .with(spotifyDataLoaderRegistryFactory.create(extractAuthorizationHeader(request)))
            .build();
    }

    private String extractAuthorizationHeader(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            var oauth2Client = oAuth2AuthorizedClientService.loadAuthorizedClient(oauth2Token.getAuthorizedClientRegistrationId(), oauth2Token.getName());
            var token = oauth2Client.getAccessToken();
            return token.getTokenType().getValue() + " " + token.getTokenValue();
        }
        return request.getHeader(AUTHORIZATION);
    }

    @Override
    public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
        throw new UnsupportedOperationException();
    }
}
