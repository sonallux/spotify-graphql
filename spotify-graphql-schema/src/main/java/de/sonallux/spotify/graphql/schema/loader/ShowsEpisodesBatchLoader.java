package de.sonallux.spotify.graphql.schema.loader;

import de.sonallux.spotify.graphql.HttpClient;
import okhttp3.HttpUrl;

public class ShowsEpisodesBatchLoader extends PagingBatchLoader {
    private static final HttpUrl BASE_URL = HttpUrl.get("https://api.spotify.com/v1/shows");//{id}/episodes";

    public ShowsEpisodesBatchLoader(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected HttpUrl.Builder urlFactory(String id) {
        return BASE_URL.newBuilder()
            .addPathSegment(id)
            .addPathSegment("episodes");
    }
}
