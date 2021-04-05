package de.sonallux.spotify.graphql.schema.loader;

import de.sonallux.spotify.graphql.HttpClient;
import okhttp3.HttpUrl;
import org.dataloader.Try;

import java.util.List;
import java.util.Map;

public class ArtistsRelatedArtistsBatchLoader extends SingleRequestBatchLoader<Map<String, List<Map<String, Object>>>, List<Map<String, Object>>> {
    private static final HttpUrl BASE_URL = HttpUrl.get("https://api.spotify.com/v1/artists");//{id}/related-artists;

    public ArtistsRelatedArtistsBatchLoader(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected HttpUrl urlFactory(String id) {
        return BASE_URL.newBuilder()
            .addPathSegment(id)
            .addPathSegment("related-artists")
            .build();
    }

    @Override
    protected Try<List<Map<String, Object>>> responseTransformation(Map<String, List<Map<String, Object>>> response) {
        return Try.succeeded(response.get("artists"));
    }
}
