package de.sonallux.spotify.graphql.schema;

public sealed interface Mapping permits FieldMapping, TypeMapping {

    Category category();

    enum Category {
        ALBUM,
        ARTIST,
        COMMON,
        EPISODE,
        PLAYLIST,
        SHOW,
        TRACK,
        USER
    }
}
