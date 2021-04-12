package de.sonallux.spotify.graphql.schema;

import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
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

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        Map<?, ?> parentObject = environment.getSource();
        var id = (String) parentObject.get("id");
        var pathParam = endpoint.getParameters().stream()
            .filter(p -> p.getLocation() == PATH)
            .findFirst()
            .orElse(null);
        if (pathParam != null && id == null) {
            throw GraphqlErrorException.newErrorException().message("Missing path parameter: " + pathParam.getName()).build();
        }

        var path = pathParam == null ? endpoint.getPath() : endpoint.getPath().replace("{" + pathParam.getName() + "}", id);
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
