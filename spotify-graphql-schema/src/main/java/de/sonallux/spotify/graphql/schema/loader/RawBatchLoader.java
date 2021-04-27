package de.sonallux.spotify.graphql.schema.loader;

import de.sonallux.spotify.graphql.HttpClient;
import okhttp3.HttpUrl;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.Try;

import java.util.Map;

public class RawBatchLoader extends SingleRequestBatchLoader<HttpUrl> {
    public RawBatchLoader(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected HttpUrl urlFactory(HttpUrl keyUrl, BatchLoaderEnvironment environment) {
        Map<String, String> context = environment.getContext();
        var builder = HttpUrl.get(context.get("baseUrl")).newBuilder();
        keyUrl.pathSegments().forEach(builder::addPathSegment);
        builder.query(keyUrl.query());
        return builder.build();
    }

    @Override
    protected Try<?> responseTransformation(Object response, Object keyContext) {
        if (keyContext instanceof Map) {
            var fieldExtraction = (String)((Map<?, ?>) keyContext).get("fieldExtraction");
            if (fieldExtraction != null) {
                if (response instanceof Map) {
                    return Try.succeeded(((Map<?, ?>) response).get(fieldExtraction));
                } else {
                    return Try.failed(getGraphQLErrorException("Can not perform fieldExtraction on " + response));
                }
            }
        }

        return Try.succeeded(response);
    }
}
