package de.sonallux.spotify.graphql.schema.generation;

import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
class EndpointMapping {
    static final SchemaObject QUERY_SCHEMA_OBJECT = new SchemaObject("Query")
        .addField(new SchemaField("album", "AlbumObject"))
        .addField(new SchemaField("albums", "Array[AlbumObject]"))
        .addField(new SchemaField("artist", "ArtistObject"))
        .addField(new SchemaField("artists", "Array[ArtistObject]"))
        .addField(new SchemaField("episode", "EpisodeObject"))
        .addField(new SchemaField("episodes", "Array[EpisodeObject]"))
        .addField(new SchemaField("playlist", "PlaylistObject"))
        .addField(new SchemaField("playlists", "Array[PlaylistObject]"))
        .addField(new SchemaField("show", "ShowObject"))
        .addField(new SchemaField("shows", "Array[ShowObject]"))
        .addField(new SchemaField("track", "TrackObject"))
        .addField(new SchemaField("tracks", "Array[TrackObject]"));

    static final List<EndpointMapping> MAPPINGS = List.of(
        new EndpointMapping("category-albums", "endpoint-get-an-albums-tracks", "AlbumObject", "tracks"),
        new EndpointMapping("category-artists", "endpoint-get-an-artists-albums", "ArtistObject", "albums"),
        new EndpointMapping("category-artists", "endpoint-get-an-artists-related-artists", "ArtistObject", "related_artists", "artists"),
        new EndpointMapping("category-artists", "endpoint-get-an-artists-top-tracks", "ArtistObject", "top_tracks", "tracks"),
        new EndpointMapping("category-playlists", "endpoint-get-playlists-tracks", "PlaylistObject", "tracks"),
        new EndpointMapping("category-shows", "endpoint-get-a-shows-episodes", "ShowObject", "episodes")
    );

    private final String categoryId;
    private final String endpointId;
    private final String objectName;
    private final String fieldName;
    private String fieldExtraction;

    EndpointMapping(String categoryId, String endpointId, String objectName, String fieldName) {
        this(categoryId, endpointId, objectName, fieldName, null);
    }

    SpotifyWebApiEndpoint getEndpoint(SpotifyWebApi spotifyWebApi) {
        return spotifyWebApi.getCategory(categoryId)
            .flatMap(category -> category.getEndpoint(endpointId))
            .orElseThrow();
    }
}
