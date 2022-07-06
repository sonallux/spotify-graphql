package de.sonallux.spotify.graphql.schema;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeMappings {
    public static final List<Mapping> ROOT_TYPE_MAPPINGS;

    static {
        ROOT_TYPE_MAPPINGS = List.of(
            new TypeMapping("CopyrightObject", TypeMapping.Category.COMMON),
            new TypeMapping("ExternalIdObject", TypeMapping.Category.COMMON),
            new TypeMapping("ExternalUrlObject", TypeMapping.Category.COMMON),
            new TypeMapping("FollowersObject", TypeMapping.Category.COMMON),
            new TypeMapping("ImageObject", TypeMapping.Category.COMMON),

            new TypeMapping("ArtistObject", TypeMapping.Category.ARTIST),
            new TypeMapping("ArtistsPagingObject", Mapping.Category.ARTIST),
            new FieldMapping("ArtistObject", "albums", "/artists/{id}/albums", Mapping.Category.ARTIST),
            new FieldMapping("ArtistObject", "related_artists", "/artists/{id}/related-artists", "artists", Mapping.Category.ARTIST),
            new FieldMapping("ArtistObject", "top_tracks", "/artists/{id}/top-tracks", "tracks", Mapping.Category.ARTIST),

            new TypeMapping("AlbumObject", TypeMapping.Category.ALBUM),
            new TypeMapping("AlbumsPagingObject", Mapping.Category.ALBUM),
            new FieldMapping("AlbumObject", "tracks", "/albums/{id}/tracks", Mapping.Category.ALBUM),

            new TypeMapping("EpisodeObject", Mapping.Category.EPISODE),
            new TypeMapping("EpisodesPagingObject", Mapping.Category.EPISODE),

            new TypeMapping("PlaylistObject", Mapping.Category.PLAYLIST),
            new TypeMapping("PlaylistsPagingObject", Mapping.Category.PLAYLIST),
            new FieldMapping("PlaylistObject", "tracks", "/playlists/{playlist_id}/tracks", Mapping.Category.PLAYLIST),

            new TypeMapping("ShowObject", Mapping.Category.SHOW),
            new TypeMapping("ShowsPagingObject", Mapping.Category.SHOW),
            new FieldMapping("ShowObject", "episodes", "/shows/{id}/episodes", Mapping.Category.SHOW),

            new TypeMapping("TrackObject", Mapping.Category.TRACK),
            new TypeMapping("LinkedTrackObject", Mapping.Category.TRACK),
            new TypeMapping("TracksPagingObject", Mapping.Category.TRACK)

        );
    }
}
