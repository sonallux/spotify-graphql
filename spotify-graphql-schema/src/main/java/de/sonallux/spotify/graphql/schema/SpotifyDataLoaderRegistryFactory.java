package de.sonallux.spotify.graphql.schema;

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
    private final HttpClient httpClient;

    public DataLoaderRegistry create(String authorizationHeader) {
        var dataLoaderOptions = DataLoaderOptions.newOptions()
            .setBatchLoaderContextProvider(() -> Map.of("authorizationHeader", authorizationHeader));
        Function<BatchLoaderWithContext<?, ?>, DataLoader<?, ?>> newDataLoader = batchLoader -> DataLoader.newDataLoader(batchLoader, dataLoaderOptions);

        var dataloaderRegistry = new DataLoaderRegistry();
        dataloaderRegistry
            .register("albumLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "albums", 20)))
            .register("albumsTracksLoader", newDataLoader.apply(new AlbumsTracksBatchLoader(httpClient)))
            .register("artistLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "artists", 50)))
            .register("artistsAlbumsLoader", newDataLoader.apply(new ArtistsAlbumsBatchLoader(httpClient)))
            .register("artistsRelatedArtistsLoader", newDataLoader.apply(new ArtistsRelatedArtistsBatchLoader(httpClient)))
            .register("artistsTopTracksLoader", newDataLoader.apply(new ArtistsTopTracksBatchLoader(httpClient)))
            .register("episodeLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "episodes", 50)))
            .register("playlistLoader", newDataLoader.apply(new PlaylistBatchLoader(httpClient)))
            .register("playlistsTracksLoader", newDataLoader.apply(new PlaylistsTracksBatchLoader(httpClient)))
            .register("showLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "shows", 50)))
            .register("showsEpisodesLoader", newDataLoader.apply(new ShowsEpisodesBatchLoader(httpClient)))
            .register("trackLoader", newDataLoader.apply(new BaseBatchLoader(httpClient, "tracks", 50)));
        return dataloaderRegistry;
    }
}
