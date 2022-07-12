package de.sonallux.spotify.graphql.schema;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeMappings {
    public static final List<Mapping> ROOT_TYPE_MAPPINGS;

    static {
        ROOT_TYPE_MAPPINGS = List.of(
            new TypeMapping("CopyrightObject", TypeMapping.Category.CORE),
            new TypeMapping("ExternalIdObject", TypeMapping.Category.CORE),
            new TypeMapping("ExternalUrlObject", TypeMapping.Category.CORE),
            new TypeMapping("FollowersObject", TypeMapping.Category.CORE),
            new TypeMapping("ImageObject", TypeMapping.Category.CORE),

            new BaseTypeQueryMapping(TypeMapping.Category.ARTIST),
            new TypeMapping("ArtistsPagingObject", Mapping.Category.ARTIST),
            new FieldMapping("ArtistObject", "albums", "/artists/{id}/albums", Mapping.Category.ARTIST),
            new FieldMapping("ArtistObject", "related_artists", "/artists/{id}/related-artists", "artists", Mapping.Category.ARTIST),
            new FieldMapping("ArtistObject", "top_tracks", "/artists/{id}/top-tracks", "tracks", Mapping.Category.ARTIST),

            new BaseTypeQueryMapping(TypeMapping.Category.ALBUM),
            new TypeMapping("AlbumsPagingObject", Mapping.Category.ALBUM),
            new FieldMapping("AlbumObject", "tracks", "/albums/{id}/tracks", Mapping.Category.ALBUM),

            new BaseTypeQueryMapping(Mapping.Category.EPISODE),
            new TypeMapping("EpisodesPagingObject", Mapping.Category.EPISODE),

            new BaseTypeQueryMapping(Mapping.Category.PLAYLIST),
            new TypeMapping("PlaylistsPagingObject", Mapping.Category.PLAYLIST),
            new FieldMapping("PlaylistObject", "tracks", "/playlists/{playlist_id}/tracks", Mapping.Category.PLAYLIST),

            new BaseTypeQueryMapping(Mapping.Category.SHOW),
            new TypeMapping("ShowsPagingObject", Mapping.Category.SHOW),
            new FieldMapping("ShowObject", "episodes", "/shows/{id}/episodes", Mapping.Category.SHOW),

            new BaseTypeQueryMapping(Mapping.Category.TRACK),
            new TypeMapping("LinkedTrackObject", Mapping.Category.TRACK),
            new TypeMapping("TracksPagingObject", Mapping.Category.TRACK),

            new FieldMapping("QueryObject", "me", "/me", Mapping.Category.USER),
            new TypeMapping("PrivateUserObject", Mapping.Category.USER),
            new FieldMapping("PrivateUserObject", "playlists", "/me/playlists", Mapping.Category.USER),
            new FieldMapping("PrivateUserObject", "top_artists", "/me/top/artists", Mapping.Category.USER),
            new FieldMapping("PrivateUserObject", "top_tracks", "/me/top/tracks", Mapping.Category.USER),
            new BaseTypeQueryMapping(Mapping.Category.USER, "PublicUserObject", true),
            new TypeMapping("PublicUserObject", Mapping.Category.USER),
            new FieldMapping("PublicUserObject", "playlists", "/users/{user_id}/playlists", Mapping.Category.USER),

            new EmptyObjectQueryMapping(Mapping.Category.LIBRARY, "library", "LibraryObject"),
            new FieldMapping("LibraryObject", "albums", "/me/albums", Mapping.Category.LIBRARY),
            new FieldMapping("LibraryObject", "episodes", "/me/episodes", Mapping.Category.LIBRARY),
            new FieldMapping("LibraryObject", "shows", "/me/shows", Mapping.Category.LIBRARY),
            new FieldMapping("LibraryObject", "tracks", "/me/tracks", Mapping.Category.LIBRARY),

            new EmptyObjectQueryMapping(Mapping.Category.BROWSE, "browse", "BrowseObject"),
            new FieldMapping("BrowseObject", "categories", "/browse/categories", "categories", Mapping.Category.BROWSE),
            new FieldMapping("BrowseObject", "category", "/browse/categories/{category_id}", null, false, Mapping.Category.BROWSE),
            new TypeMapping("CategoryObject", Mapping.Category.BROWSE),
            new FieldMapping("CategoryObject", "playlists", "/browse/categories/{category_id}/playlists", "playlists", Mapping.Category.BROWSE),
            new FieldMapping("BrowseObject", "featured_playlists", "/browse/featured-playlists", Mapping.Category.BROWSE),
            new FieldMapping("BrowseObject", "new_releases", "/browse/new-releases", "albums", Mapping.Category.BROWSE),
            new FieldMapping("BrowseObject", "recommendations", "/recommendations", Mapping.Category.BROWSE),
            new FieldMapping("BrowseObject", "recommendations_genre_seeds", "/recommendations/available-genre-seeds", "genres", Mapping.Category.BROWSE)
        );
    }
}
