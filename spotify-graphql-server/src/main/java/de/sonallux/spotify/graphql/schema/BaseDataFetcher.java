package de.sonallux.spotify.graphql.schema;

import de.sonallux.spotify.graphql.SpotifyUtil;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BaseDataFetcher implements DataFetcher<Object> {

    private final String type;

    @Override
    public Object get(DataFetchingEnvironment environment) {
        String id = environment.getArgument("id");
        String uri = environment.getArgument("uri");
        if (id != null && uri == null) {
            // id present
            return environment.getDataLoader(type + "Loader").load(id);
        } else if (id == null && uri != null) {
            // uri present
            if (!type.equals(SpotifyUtil.getTypeFromUri(uri))) {
                throw GraphqlErrorException.newErrorException()
                    .message("Expected a 'uri' of type '" + type + "' but got 'uri' " + uri)
                    .build();
            }
            id = SpotifyUtil.getIdFromUri(uri);
            if (id == null) {
                throw GraphqlErrorException.newErrorException()
                    .message("Missing 'id' in provided 'uri' " + uri)
                    .build();
            }
            return environment.getDataLoader(type + "Loader").load(id);
        } else {
            throw GraphqlErrorException.newErrorException()
                .message("Either an 'id' or 'uri' must be specified")
                .build();
        }
    }
}
