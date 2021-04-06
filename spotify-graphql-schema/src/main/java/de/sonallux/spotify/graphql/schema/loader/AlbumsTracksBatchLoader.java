package de.sonallux.spotify.graphql.schema.loader;

import de.sonallux.spotify.graphql.HttpClient;
import okhttp3.HttpUrl;

public class AlbumsTracksBatchLoader extends PagingBatchLoader {
    private static final HttpUrl BASE_URL = HttpUrl.get("https://api.spotify.com/v1/albums");//{id}/tracks";

    public AlbumsTracksBatchLoader(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected HttpUrl.Builder urlFactory(String id) {
        return BASE_URL.newBuilder()
            .addPathSegment(id)
            .addPathSegment("tracks");
    }
}
