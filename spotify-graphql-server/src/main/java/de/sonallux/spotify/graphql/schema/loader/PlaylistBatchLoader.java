package de.sonallux.spotify.graphql.schema.loader;

import de.sonallux.spotify.graphql.HttpClient;
import okhttp3.HttpUrl;
import org.dataloader.Try;

import java.util.Map;

public class PlaylistBatchLoader extends SingleRequestBatchLoader<Map<String, Object>, Map<String, Object>> {
    private static final HttpUrl BASE_URL = HttpUrl.get("https://api.spotify.com/v1/playlists");//{id}?additional_types=track,episode";

    public PlaylistBatchLoader(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected HttpUrl urlFactory(String id) {
        return BASE_URL.newBuilder()
            .addPathSegment(id)
            .addQueryParameter("additional_types", "track,episode")
            .build();
    }

    @Override
    protected Try<Map<String, Object>> responseTransformation(Map<String, Object> object) {
        return wrapSpotifyBaseObject(object);
    }
}
