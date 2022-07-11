package de.sonallux.spotify.graphql.schema;

public sealed interface Mapping permits FieldMapping, TypeMapping, BaseTypeQueryMapping, EmptyObjectQueryMapping {

    Category category();

    enum Category {
        ALBUM,
        ARTIST,
        BROWSE,
        CORE,
        EPISODE,
        LIBRARY,
        PLAYLIST,
        SHOW,
        TRACK,
        USER
    }
}
