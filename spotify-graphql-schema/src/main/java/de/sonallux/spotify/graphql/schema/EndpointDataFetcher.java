package de.sonallux.spotify.graphql.schema;

import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import graphql.GraphQLException;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.AllArgsConstructor;
import okhttp3.HttpUrl;

import java.util.Map;

import static de.sonallux.spotify.core.model.SpotifyWebApiEndpoint.ParameterLocation.PATH;
import static de.sonallux.spotify.core.model.SpotifyWebApiEndpoint.ParameterLocation.QUERY;

@AllArgsConstructor
public class EndpointDataFetcher implements DataFetcher<Object> {
    private final SpotifyWebApiEndpoint endpoint;
    private final String fieldExtraction;
    private final boolean isIdProvidedByParent;

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        var pathParam = endpoint.getParameters().stream()
            .filter(p -> p.getLocation() == PATH)
            .findFirst()
            .orElse(null);

        String path;
        if (isIdProvidedByParent) {
            if (pathParam == null) {
                throw GraphqlErrorException.newErrorException().message("Missing path parameter").build();
            }
            Map<?, ?> parentObject = environment.getSource();
            var id = (String) parentObject.get("id");
            path = endpoint.getPath().replace("{" + pathParam.getName() + "}", id);
        } else if (pathParam != null) {
            String id = environment.getArgument(pathParam.getName());
            path = endpoint.getPath().replace("{" + pathParam.getName() + "}", id);
        } else {
            path = endpoint.getPath();
        }

        var urlBuilder = HttpUrl.get("https://base-url" + path).newBuilder();

        //TODO: check required or optional parameters
        for (var param : endpoint.getParameters()) {
            if (param.getLocation() == QUERY) {
                if (param.getName().equals("additional_types")) {
                    urlBuilder.addQueryParameter("additional_types", "track,episode");
                } else if (environment.containsArgument(param.getName())) {
                    urlBuilder.addQueryParameter(param.getName(), environment.getArgument(param.getName()).toString());
                } else if (param.getName().equals("market") && param.isRequired()) {
                    urlBuilder.addQueryParameter("market", "from_token");
                }
            }
        }

        return environment.getDataLoader("rawLoader")
            .load(urlBuilder.build(), fieldExtraction != null ? Map.of("fieldExtraction", fieldExtraction) : Map.of());
    }
}
