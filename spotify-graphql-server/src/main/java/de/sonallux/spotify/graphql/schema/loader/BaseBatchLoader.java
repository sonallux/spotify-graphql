package de.sonallux.spotify.graphql.schema.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
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
public class BaseBatchLoader extends AbstractBatchLoader<String, Map<String, Object>> {

    private static final HttpUrl BASE_URL = HttpUrl.get("https://api.spotify.com/v1");//{type}?ids={ids}

    private final HttpClient httpClient;
    private final String type;
    private final int maxIdsPerRequest;

    @Override
    protected List<Try<Map<String, Object>>> loadBatch(List<String> list, BatchLoaderEnvironment environment) {
        var responseType = new TypeReference<Map<String, List<Map<String, Object>>>>() {};

        var result = new ArrayList<Try<Map<String, Object>>>();
        for (var subList : Lists.partition(list, maxIdsPerRequest)) {
            try {
                var url = BASE_URL.newBuilder()
                    .addPathSegment(type)
                    .addQueryParameter("ids", String.join(",", subList))
                    .build();
                var response = httpClient.request(getRequestBuilder(environment).url(url).build(), responseType);
                response.get(type).stream().map(this::wrapSpotifyBaseObject).forEach(result::add);
            }
            catch (IOException e) {
                var exception = getGraphQLErrorException(e);
                for (int i = 0; i < subList.size(); i++) {
                    result.add(Try.failed(exception));
                }
            }
        }
        return result;
    }
}
