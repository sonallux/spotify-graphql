package de.sonallux.spotify.graphql.schema.loader;

import de.sonallux.spotify.graphql.HttpClient;
import okhttp3.HttpUrl;

public class ArtistsAlbumsBatchLoader extends PagingBatchLoader {
    private static final HttpUrl BASE_URL = HttpUrl.get("https://api.spotify.com/v1/artists");//{id}/albums";

    public ArtistsAlbumsBatchLoader(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected HttpUrl.Builder urlFactory(String id) {
        return BASE_URL.newBuilder()
            .addPathSegment(id)
            .addPathSegment("albums");
    }
}
