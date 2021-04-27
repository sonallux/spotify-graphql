package de.sonallux.spotify.graphql.schema;

import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.graphql.HttpClient;
import de.sonallux.spotify.graphql.schema.loader.*;
import lombok.AllArgsConstructor;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;

import java.util.Map;
import java.util.function.Function;

@AllArgsConstructor
public class SpotifyDataLoaderRegistryFactory {
    private final SpotifyWebApi spotifyWebApi;
    private final HttpClient httpClient;

    public DataLoaderRegistry create(String authorizationHeader) {
        var dataLoaderOptions = DataLoaderOptions.newOptions()
            .setBatchLoaderContextProvider(() -> Map.of(
                "authorizationHeader", authorizationHeader,
                "baseUrl", spotifyWebApi.getEndpointUrl()
            ));
        Function<BatchLoaderWithContext<?, ?>, DataLoader<?, ?>> newDataLoader = batchLoader -> DataLoader.newDataLoader(batchLoader, dataLoaderOptions);

        var dataloaderRegistry = new DataLoaderRegistry();
        dataloaderRegistry
            .register("albumLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "albums", 20)))
            .register("artistLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "artists", 50)))
            .register("episodeLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "episodes", 50)))
            .register("playlistLoader", newDataLoader.apply(new PlaylistBatchLoader(httpClient)))
            .register("showLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "shows", 50)))
            .register("trackLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "tracks", 50)))
            .register("rawLoader", newDataLoader.apply(new RawBatchLoader(httpClient)))
            .register("mutationEndpointLoader", newDataLoader.apply(new MutationEndpointLoader(httpClient)));
        return dataloaderRegistry;
    }
}
