package de.sonallux.spotify.graphql.schema;

import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContextBuilder;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
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

    @Override
    public GraphQLContext build(HttpServletRequest request, HttpServletResponse response) {
        return SpotifyGraphQLContext.createServletContext()
            .with(request)
            .with(request)
            .with(spotifyDataLoaderRegistryFactory.create(request.getHeader(AUTHORIZATION)))
            .build();
    }

    @Override
    public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
        throw new UnsupportedOperationException();
    }
}
