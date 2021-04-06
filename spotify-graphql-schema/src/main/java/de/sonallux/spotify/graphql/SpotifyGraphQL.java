package de.sonallux.spotify.graphql;

import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.graphql.schema.SchemaCreator;
import de.sonallux.spotify.graphql.schema.SpotifyDataLoaderRegistryFactory;
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

    public SpotifyGraphQL() throws IOException {
        spotifyGraphQLSchema = new SchemaCreator().generate(SpotifyWebApiUtils.load());
        spotifyGraphQL = GraphQL.newGraphQL(spotifyGraphQLSchema).build();
        spotifyDataLoaderRegistryFactory = new SpotifyDataLoaderRegistryFactory(new HttpClient());
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
