package de.sonallux.spotify.graphql.schema.loader;

import de.sonallux.spotify.graphql.HttpClient;
import okhttp3.HttpUrl;
import org.dataloader.Try;

import java.util.List;
import java.util.Map;

public class ArtistsTopTracksBatchLoader extends SingleRequestBatchLoader<Map<String, List<Map<String, Object>>>, List<Map<String, Object>>> {
    private static final HttpUrl BASE_URL = HttpUrl.get("https://api.spotify.com/v1/artists");//{id}/top-tracks;

    public ArtistsTopTracksBatchLoader(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected HttpUrl urlFactory(String id) {
        return BASE_URL.newBuilder()
            .addPathSegment(id)
            .addPathSegment("top-tracks")
            .addQueryParameter("country", "from_token")
            .build();
    }

    @Override
    protected Try<List<Map<String, Object>>> responseTransformation(Map<String, List<Map<String, Object>>> response) {
        return Try.succeeded(response.get("tracks"));
    }
}
