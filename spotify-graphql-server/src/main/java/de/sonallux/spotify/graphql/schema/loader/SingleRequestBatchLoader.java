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
abstract class SingleRequestBatchLoader<RESPONSE_TYPE, RETURN_TYPE> extends AbstractBatchLoader<String, RETURN_TYPE> {
    private final HttpClient httpClient;

    protected abstract HttpUrl urlFactory(String item);

    protected abstract Try<RETURN_TYPE> responseTransformation(RESPONSE_TYPE response);

    @Override
    protected List<Try<RETURN_TYPE>> loadBatch(List<String> list, BatchLoaderEnvironment env) {
        var responseType = new TypeReference<RESPONSE_TYPE>() {};

        var result = new ArrayList<Try<RETURN_TYPE>>();
        for (var item : list) {
            try {
                var url = urlFactory(item);
                var response = httpClient.request(getRequestBuilder(env).url(url).build(), responseType);
                var object = responseTransformation(response);
                result.add(object);
            } catch (IOException e) {
                result.add(Try.failed(getGraphQLErrorException(e)));
            }
        }

        return result;
    }
}
