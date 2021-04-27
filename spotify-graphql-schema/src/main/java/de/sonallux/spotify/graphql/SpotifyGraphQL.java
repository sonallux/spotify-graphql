package de.sonallux.spotify.graphql;

import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.graphql.schema.SpotifyDataLoaderRegistryFactory;
import de.sonallux.spotify.graphql.schema.generation.SpotifyGraphQLSchemaGenerator;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;

import java.io.IOException;

public class SpotifyGraphQL {
    private final SpotifyDataLoaderRegistryFactory spotifyDataLoaderRegistryFactory;
    private final GraphQLSchema spotifyGraphQLSchema;
    private final GraphQL spotifyGraphQL;

    public SpotifyGraphQL(SpotifyWebApi spotifyWebApi) {
        spotifyGraphQLSchema = new SpotifyGraphQLSchemaGenerator().generate(spotifyWebApi);
        spotifyGraphQL = GraphQL.newGraphQL(spotifyGraphQLSchema).build();
        spotifyDataLoaderRegistryFactory = new SpotifyDataLoaderRegistryFactory(spotifyWebApi, new HttpClient());
    }

    public SpotifyGraphQL() throws IOException {
        this(SpotifyWebApiUtils.load());
    }

    public String printSchema() {
        return new SchemaPrinter().print(spotifyGraphQLSchema);
    }

    public ExecutionResult execute(String query, String authorizationHeader) {
        var executionInput = ExecutionInput
            .newExecutionInput(query)
            .dataLoaderRegistry(spotifyDataLoaderRegistryFactory.create(authorizationHeader))
            .build();
        return spotifyGraphQL.execute(executionInput);
    }
}
