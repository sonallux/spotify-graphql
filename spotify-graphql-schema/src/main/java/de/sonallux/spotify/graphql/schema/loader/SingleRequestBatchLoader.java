package de.sonallux.spotify.graphql.schema.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import de.sonallux.spotify.graphql.HttpClient;
import lombok.AllArgsConstructor;
import okhttp3.HttpUrl;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.Try;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
abstract class SingleRequestBatchLoader<KEY_TYPE> extends AbstractBatchLoader<KEY_TYPE> {
    private final HttpClient httpClient;

    protected abstract HttpUrl urlFactory(KEY_TYPE item, BatchLoaderEnvironment environment);

    protected Try<?> responseTransformation(Object response, Object keyContext) {
        return Try.succeeded(response);
    }

    @Override
    protected List<Try<?>> loadBatch(List<KEY_TYPE> list, BatchLoaderEnvironment environment) {
        var responseType = new TypeReference<>() {};

        var result = new ArrayList<Try<?>>();
        for (var item : list) {
            try {
                var url = urlFactory(item, environment);
                var response = httpClient.request(getRequestBuilder(environment).url(url).build(), responseType);
                var object = responseTransformation(response, environment.getKeyContexts().get(item));
                result.add(object);
            } catch (IOException e) {
                result.add(Try.failed(getGraphQLErrorException(e)));
            }
        }

        return result;
    }
}
