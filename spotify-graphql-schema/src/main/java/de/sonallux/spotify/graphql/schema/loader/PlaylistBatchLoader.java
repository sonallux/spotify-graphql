package de.sonallux.spotify.graphql.schema.loader;

import de.sonallux.spotify.graphql.HttpClient;
import okhttp3.HttpUrl;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.Try;

import java.util.Map;

public class PlaylistBatchLoader extends SingleRequestBatchLoader<String> {
    public PlaylistBatchLoader(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected HttpUrl urlFactory(String id, BatchLoaderEnvironment environment) {
        return getUrlBuilder(environment)
            .addPathSegment("playlists")
            .addPathSegment(id)
            .addQueryParameter("additional_types", "track,episode")
            .build();
    }
}
