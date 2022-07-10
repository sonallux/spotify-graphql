package de.sonallux.spotify.graphql.schema;

public sealed interface Mapping permits FieldMapping, TypeMapping, BaseTypeQueryMapping, EmptyObjectQueryMapping {

    Category category();

    enum Category {
        CORE,
        ALBUM,
        ARTIST,
        EPISODE,
        LIBRARY,
        PLAYLIST,
        SHOW,
        TRACK,
        USER
    }
}
