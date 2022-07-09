package de.sonallux.spotify.graphql.schema;

public sealed interface Mapping permits FieldMapping, TypeMapping, BaseTypeQueryMapping {

    Category category();

    enum Category {
        CORE,
        ALBUM,
        ARTIST,
        EPISODE,
        PLAYLIST,
        SHOW,
        TRACK,
        USER
    }
}
