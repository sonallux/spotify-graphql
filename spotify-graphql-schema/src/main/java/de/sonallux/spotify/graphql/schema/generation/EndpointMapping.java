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
        new EndpointMapping("category-albums", "endpoint-get-an-albums-tracks", "AlbumObject", "tracks").isIdProvidedByParent(true),
        new EndpointMapping("category-artists", "endpoint-get-an-artists-albums", "ArtistObject", "albums").isIdProvidedByParent(true),
        new EndpointMapping("category-artists", "endpoint-get-an-artists-related-artists", "ArtistObject", "related_artists").isIdProvidedByParent(true).fieldExtraction("artists"),
        new EndpointMapping("category-artists", "endpoint-get-an-artists-top-tracks", "ArtistObject", "top_tracks").isIdProvidedByParent(true).fieldExtraction("tracks"),
        new EndpointMapping("category-playlists", "endpoint-get-playlists-tracks", "PlaylistObject", "tracks").isIdProvidedByParent(true),
        new EndpointMapping("category-shows", "endpoint-get-a-shows-episodes", "ShowObject", "episodes").isIdProvidedByParent(true),
        new EndpointMapping("category-users-profile", "endpoint-get-current-users-profile", "Query", "me"),
        new EndpointMapping("category-users-profile", "endpoint-get-users-profile", "Query", "user"),
        new EndpointMapping("category-markets", "endpoint-get-available-markets", "Query", "markets").fieldExtraction("markets"),
        new EndpointMapping("category-browse", "endpoint-get-new-releases", "Query", "new_releases").fieldExtraction("albums"),
        new EndpointMapping("category-browse", "endpoint-get-featured-playlists", "Query", "featured_playlists"),
        new EndpointMapping("category-browse", "endpoint-get-categories", "Query", "categories").fieldExtraction("categories"),
        new EndpointMapping("category-browse", "endpoint-get-a-category", "Query", "category"),
        new EndpointMapping("category-browse", "endpoint-get-a-categories-playlists", "CategoryObject", "playlists").isIdProvidedByParent(true).fieldExtraction("playlists"),
        new EndpointMapping("category-browse", "endpoint-get-recommendations", "Query", "recommendation"),
        new EndpointMapping("category-browse", "endpoint-get-recommendation-genres", "Query", "recommendation_genres").fieldExtraction("genres")

    );

    private final String categoryId;
    private final String endpointId;
    private final String objectName;
    private final String fieldName;
    private String fieldExtraction;
    private boolean isIdProvidedByParent;

    EndpointMapping(String categoryId, String endpointId, String objectName, String fieldName) {
        this(categoryId, endpointId, objectName, fieldName, null, false);
    }

    EndpointMapping fieldExtraction(String fieldExtraction) {
        this.fieldExtraction = fieldExtraction;
        return this;
    }

    EndpointMapping isIdProvidedByParent(boolean isIdProvidedByParent) {
        this.isIdProvidedByParent = isIdProvidedByParent;
        return this;
    }

    SpotifyWebApiEndpoint getEndpoint(SpotifyWebApi spotifyWebApi) {
        return spotifyWebApi.getCategory(categoryId)
            .flatMap(category -> category.getEndpoint(endpointId))
            .orElseThrow();
    }
}
