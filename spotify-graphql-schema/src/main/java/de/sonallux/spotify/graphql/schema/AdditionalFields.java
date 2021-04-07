package de.sonallux.spotify.graphql.schema;

import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import de.sonallux.spotify.core.model.SpotifyWebApiObject;
import graphql.schema.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.Scalars.*;

@Slf4j
public class AdditionalFields {
    public static final List<AdditionalField> ADDITIONAL_FIELDS = List.of(
        new AdditionalField("AlbumObject", "tracks", "category-albums", "endpoint-get-an-albums-tracks", PagingDataFetcher.factory("albumsTracksLoader")),
        new AdditionalField("ArtistObject", "albums", "category-artists", "endpoint-get-an-artists-albums", PagingDataFetcher.factory("artistsAlbumsLoader"), new SpotifyWebApiObject.Property("albums", "PagingAlbumObject")),
        new AdditionalField("ArtistObject", "related_artists", "category-artists", "endpoint-get-an-artists-related-artists", DelegateToLoaderDataFetcher.factory("artistsRelatedArtistsLoader"), new SpotifyWebApiObject.Property("related_artists", "Array[ArtistObject]")),
        new AdditionalField("ArtistObject", "top_tracks", "category-artists", "endpoint-get-an-artists-top-tracks", DelegateToLoaderDataFetcher.factory("artistsTopTracksLoader"), new SpotifyWebApiObject.Property("top_tracks", "Array[TrackObject]")),
        new AdditionalField("PlaylistObject", "tracks", "category-playlists", "endpoint-get-playlists-tracks", PagingDataFetcher.factory("playlistsTracksLoader")),
        new AdditionalField("ShowObject", "episodes", "category-shows", "endpoint-get-a-shows-episodes", PagingDataFetcher.factory("showsEpisodesLoader"))
    );

    private static final List<String> PAGING_PARAMETERS = List.of("limit", "offset");

    public static void registerAdditionalDataFetcher(GraphQLCodeRegistry.Builder codeRegistryBuilder) {
        for (var field : ADDITIONAL_FIELDS) {
            if (field.dataFetcherFactory == null) {
                continue;
            }
            var coordinates = FieldCoordinates.coordinates(field.getGraphQLObjectName(), field.getPropertyName());
            codeRegistryBuilder.dataFetcher(coordinates, field.dataFetcherFactory);
        }
    }

    public static List<GraphQLArgument> getPagingArguments(SpotifyWebApi spotifyWebApi, String spotifyObject, String property) {
        var field = ADDITIONAL_FIELDS.stream()
            .filter(f -> f.getSpotifyName().equals(spotifyObject) && f.getPropertyName().equals(property))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Can not get additional field " + property + " for " + spotifyObject));


        var endpoint = field.getEndpoint(spotifyWebApi)
            .orElseThrow(() -> new RuntimeException("Can not get endpoint " + field.getEndpointId()));

        return endpoint.getParameters().stream()
            .filter(p -> p.getLocation() == SpotifyWebApiEndpoint.ParameterLocation.QUERY)
            .filter(p -> PAGING_PARAMETERS.contains(p.getName()))
            .map(p -> GraphQLArgument.newArgument()
                .name(p.getName())
                .type(getGraphQLType(p.getType()))
                .description(p.getDescription())
                .build()
            ).collect(Collectors.toList());
    }

    private static GraphQLInputType getGraphQLType(String type) {
        if ("Boolean".equals(type)) {
            return GraphQLBoolean;
        } else if ("Float".equals(type)) {
            return GraphQLFloat;
        } else if ("Integer".equals(type)) {
            return GraphQLInt;
        } else if ("String".equals(type)) {
            return GraphQLString;
        } else if ("Timestamp".equals(type)) {
            return GraphQLString;
        }
        log.warn("Can not map type '" + type + "' to a GraphQLInputType");
        return null;
    }

    public static Stream<SpotifyWebApiObject.Property> getAdditionalProperties(SpotifyWebApi spotifyWebApi, String spotifyObject) {
        return ADDITIONAL_FIELDS.stream()
            .filter(f -> f.getSpotifyName().equals(spotifyObject))
            .map(AdditionalField::getProperty)
            .filter(Objects::nonNull);
    }

    @Getter
    @AllArgsConstructor
    private static class AdditionalField {
        private final String spotifyName;
        private final String propertyName;
        private final String categoryId;
        private final String endpointId;
        private final DataFetcherFactory<?> dataFetcherFactory;
        private final SpotifyWebApiObject.Property property;

        public AdditionalField(String spotifyName, String propertyName, String categoryId, String endpointId, DataFetcherFactory<?> dataFetcherFactory) {
            this(spotifyName, propertyName, categoryId, endpointId, dataFetcherFactory,null);
        }

        public String getGraphQLObjectName() {
            return spotifyName.replace("Object", "");
        }

        public Optional<SpotifyWebApiEndpoint> getEndpoint(SpotifyWebApi spotifyWebApi) {
            return spotifyWebApi.getCategory(getCategoryId())
                .flatMap(category -> category.getEndpoint(getEndpointId()));
        }
    }
}
