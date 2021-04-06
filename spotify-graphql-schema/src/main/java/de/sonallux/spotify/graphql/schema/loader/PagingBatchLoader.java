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
import java.util.Map;

@AllArgsConstructor
abstract class PagingBatchLoader extends AbstractBatchLoader<Map<String, String>, Map<String, Object>> {
    private final HttpClient httpClient;

    protected abstract HttpUrl.Builder urlFactory(String id);

    @Override
    protected List<Try<Map<String, Object>>> loadBatch(List<Map<String, String>> list, BatchLoaderEnvironment env) {
        var responseType = new TypeReference<Map<String, Object>>() {};

        var result = new ArrayList<Try<Map<String, Object>>>();
        for (var requestData : list) {
            var id = requestData.get("id");

            try {
                var url = urlFactory(id);

                if (requestData.containsKey("limit")) {
                    url.addQueryParameter("limit", requestData.get("limit"));
                }

                if (requestData.containsKey("offset")) {
                    url.addQueryParameter("offset", requestData.get("offset"));
                }

                var response = httpClient.request(getRequestBuilder(env).url(url.build()).build(), responseType);
                result.add(Try.succeeded(response));
            } catch (IOException e) {
                result.add(Try.failed(getGraphQLErrorException(e)));
            }
        }

        return result;
    }
}
