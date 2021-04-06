package de.sonallux.spotify.graphql;

import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.graphql.schema.SchemaCreator;
import de.sonallux.spotify.graphql.schema.SpotifyDataLoaderRegistryFactory;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;

import java.io.IOException;

public class SpotifyGraphQL {
    private final SpotifyDataLoaderRegistryFactory spotifyDataLoaderRegistryFactory;
    private final GraphQL spotifyGraphQL;

    public SpotifyGraphQL() throws IOException {
        var schema = new SchemaCreator().generate(SpotifyWebApiUtils.load());
        spotifyGraphQL = GraphQL.newGraphQL(schema).build();
        spotifyDataLoaderRegistryFactory = new SpotifyDataLoaderRegistryFactory(new HttpClient());
    }

    public ExecutionResult execute(String query, String authorizationHeader) {
        var executionInput = ExecutionInput
            .newExecutionInput(query)
            .dataLoaderRegistry(spotifyDataLoaderRegistryFactory.create(authorizationHeader))
            .build();
        return spotifyGraphQL.execute(executionInput);
    }
}
