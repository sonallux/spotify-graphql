package de.sonallux.spotify.graphql.schema;

import de.sonallux.spotify.graphql.SpotifyUtil;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import static graphql.GraphqlErrorException.newErrorException;

@AllArgsConstructor
public class BaseObjectsDataFetcher implements DataFetcher<Object> {
    private final String type;

    @Override
    public Object get(DataFetchingEnvironment environment) {
        List<String> ids = environment.getArgument("ids");
        List<String> uris = environment.getArgument("uris");
        if (ids != null && uris == null) {
            // id present
            return environment.<String, Object>getDataLoader(type + "Loader").loadMany(ids);
        } else if (ids == null && uris != null) {
            // uri present
            ids = uris.stream()
                .map(uri -> {
                    if (!type.equals(SpotifyUtil.getTypeFromUri(uri))) {
                        throw newErrorException()
                            .message("Expected a 'uri' of type '" + type + "' but got 'uri' " + uri)
                            .build();
                    }
                    var id = SpotifyUtil.getIdFromUri(uri);
                    if (id == null) {
                        throw newErrorException()
                            .message("Missing 'id' in provided 'uri' " + uri)
                            .build();
                    }
                    return id;
                })
                .collect(Collectors.toList());
            return environment.<String, Object>getDataLoader(type + "Loader").loadMany(ids);
        } else {
            throw newErrorException()
                .message("Either an 'id' or 'uri' must be specified")
                .build();
        }
    }
}
