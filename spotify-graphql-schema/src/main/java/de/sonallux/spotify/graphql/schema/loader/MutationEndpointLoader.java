package de.sonallux.spotify.graphql.schema.loader;

import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import de.sonallux.spotify.graphql.HttpClient;
import lombok.AllArgsConstructor;
import okhttp3.RequestBody;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.Try;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class MutationEndpointLoader extends AbstractBatchLoader<SpotifyWebApiEndpoint> {
    private final HttpClient httpClient;

    @Override
    protected List<Try<?>> loadBatch(List<SpotifyWebApiEndpoint> list, BatchLoaderEnvironment environment) {
        var result = new ArrayList<Try<?>>();
        for (var endpoint : list) {
            try {
                result.add(Try.succeeded(executeRequest(endpoint, environment)));
            } catch (IOException e) {
                result.add(Try.failed(getGraphQLErrorException(e)));
            }
        }

        return result;
    }

    private Object executeRequest(SpotifyWebApiEndpoint endpoint, BatchLoaderEnvironment environment) throws IOException {
        @SuppressWarnings("unchecked")
        var input = (Map<String, Object>) environment.getKeyContexts().get(endpoint);

        var urlBuilder = getUrlBuilder(environment);
        for (var pathSegment : endpoint.getPath().split("/")) {
            if (pathSegment.isBlank()) {
                continue;
            }
            if (pathSegment.charAt(0) == '{') {
                var paramName = pathSegment.substring(1, pathSegment.length() - 1);
                var paramValue = (String)input.get(paramName);
                urlBuilder.addPathSegment(paramValue);
            } else {
                urlBuilder.addPathSegment(pathSegment);
            }
        }

        var bodyParameters = new HashMap<String, Object>();
        for (var parameter : endpoint.getParameters()) {
            switch (parameter.getLocation()) {
                case QUERY -> {
                    if (input.containsKey(parameter.getName())) {
                        var paramValue = (String) input.get(parameter.getName());
                        urlBuilder.addQueryParameter(parameter.getName(), paramValue);
                    } else if (parameter.isRequired()) {
                        throw getGraphQLErrorException("Missing required query parameter: " + parameter.getName());
                    }
                }
                case BODY -> {
                    if (input.containsKey(parameter.getName())) {
                        var paramValue = input.get(parameter.getName());
                        bodyParameters.put(parameter.getName(), paramValue);
                    } else if (parameter.isRequired()) {
                        throw getGraphQLErrorException("Missing required body parameter: " + parameter.getName());
                    }
                }
            }
        }

        var requestBody = bodyParameters.size() > 0 ? httpClient.createJsonBody(bodyParameters) : RequestBody.create(new byte[0]);

        var requestBuilder = getRequestBuilder(environment)
            .url(urlBuilder.build())
            .method(endpoint.getHttpMethod(), requestBody);

        try (var response = httpClient.request(requestBuilder.build())) {
            return Map.of("status", response.code());
        }
        catch (IOException e) {
            throw getGraphQLErrorException(e);
        }
    }
}
