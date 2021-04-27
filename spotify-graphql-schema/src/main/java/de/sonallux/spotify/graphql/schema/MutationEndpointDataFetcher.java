package de.sonallux.spotify.graphql.schema;

import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.DataFetchingEnvironment;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class MutationEndpointDataFetcher implements DataFetcher<Object> {
    private final SpotifyWebApiEndpoint endpoint;

    public static DataFetcherFactory<Object> factory(SpotifyWebApiEndpoint endpoint) {
        return (env) -> new MutationEndpointDataFetcher(endpoint);
    }

    @Override
    public Object get(DataFetchingEnvironment dataFetchingEnvironment)  {
        Map<String, Object> input = dataFetchingEnvironment.getArgument("input");
        return dataFetchingEnvironment.getDataLoader("mutationEndpointLoader").load(endpoint, input);
    }
}
