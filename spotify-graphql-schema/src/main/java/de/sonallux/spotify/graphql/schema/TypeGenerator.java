package de.sonallux.spotify.graphql.schema;

import com.google.common.base.Strings;
import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.core.model.SpotifyWebApiObject;
import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.Scalars.*;

@Slf4j
@RequiredArgsConstructor
class TypeGenerator {
    private static final List<String> BASE_SPOTIFY_TYPES = List.of("AlbumObject", "ArtistObject", "EpisodeObject", "PlaylistObject", "PlaylistTrackObject", "TrackObject", "ShowObject");

    private final SpotifyWebApi spotifyWebApi;

    private Queue<String> queue; // SpotifyObject name
    private SortedMap<String, GraphQLType> graphQLTypes; // Spotify type -> GraphQLType
    private SpotifyWebApiObject spotifyPagingObject;
    private SpotifyWebApiObject spotifyCursorPagingObject;

    Collection<GraphQLType> generate() {
        this.queue = new LinkedList<>(BASE_SPOTIFY_TYPES);
        this.graphQLTypes = new TreeMap<>();

        this.spotifyPagingObject = spotifyWebApi.getObject("PagingObject").orElseThrow();
        this.spotifyCursorPagingObject = spotifyWebApi.getObject("CursorPagingObject").orElseThrow();

        this.iterate();

        return graphQLTypes.values();
    }

    private void iterate() {
        while (!queue.isEmpty()) {
            var currentObject = queue.poll();
            if (graphQLTypes.containsKey(currentObject)) {
                continue;
            }

            var spotifyObject = spotifyWebApi.getObject(currentObject).orElseThrow();
            var graphQLObject = generateObject(spotifyObject);

            if (graphQLObject.getFieldDefinitions().size() == 0) {
                log.warn(currentObject + " has zero properties");
            }

            this.graphQLTypes.put(currentObject, graphQLObject);
        }
    }

    private GraphQLObjectType generateObject(SpotifyWebApiObject spotifyObject) {
        String description = null;
        if (!Strings.isNullOrEmpty(spotifyObject.getLink())) {
            description = String.format("[%s](%s)", spotifyObject.getName(), spotifyObject.getLink());
        }

        var fields = Stream
            .concat(spotifyObject.getProperties().stream(), AdditionalFields.getAdditionalProperties(spotifyWebApi, spotifyObject.getName()))
            .map(p -> generateField(spotifyObject.getName(), p))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return GraphQLObjectType.newObject()
            .name(toGraphQLName(spotifyObject.getName()))
            .description(description)
            .fields(fields)
            .build();
    }

    private GraphQLFieldDefinition generateField(String objectName, SpotifyWebApiObject.Property property) {
        if ("external_urls".equals(property.getName())) {
            //The type if this field is a map, which can not be mapped easily to GraphQL as the keys are not known
            return null;
        }

        var builder = GraphQLFieldDefinition.newFieldDefinition()
            // Every field definition will get its onw instance of the default PropertyDataFetcher when
            // no dataFetcherFactory is set. Unfortunately the dataFetcher of the field takes precedence over
            // the defaultDataFetcher of the GraphQLCodeRegistry.
            // see issue: https://github.com/graphql-java/graphql-java/issues/2145
            .dataFetcherFactory(env -> SpotifyPropertyDataFetcher.fetching(env.getFieldDefinition().getName()))
            .description(property.getDescription().replace("\"", "'"));

        if ("type".equals(property.getName())) {
            builder.name("spotify_type");
        } else {
            builder.name(property.getName());
        }

        if (SpotifyWebApiUtils.PAGING_OBJECT_TYPE_PATTERN.matcher(property.getType()).matches()) {
            builder.arguments(AdditionalFields.getPagingArguments(spotifyWebApi, objectName, property.getName()));
        }

        var graphQLType = toGraphQLType(property.getType());
        if (graphQLType == null) {
            return null;
        }
        return builder.type(graphQLType).build();
    }

    private GraphQLOutputType toGraphQLType(String type) {
        Matcher matcher;
        if ("Boolean".equals(type)) {
            return GraphQLBoolean;
        } else if ("Float".equals(type)) {
            return GraphQLFloat;
        } else if ("Integer".equals(type)) {
            return GraphQLInt;
        } else if ("Object".equals(type)) {
            log.warn("Can not map type 'Object' to a GraphQLType");
            return null;
        } else if ("String".equals(type)) {
            return GraphQLString;
        } else if ("Timestamp".equals(type)) {
            return GraphQLString;
        } else if ((matcher = SpotifyWebApiUtils.ARRAY_TYPE_PATTERN.matcher(type)).matches()) {
            var itemType = toGraphQLType(matcher.group(1));
            return GraphQLList.list(itemType);
        } else if ((matcher = SpotifyWebApiUtils.PAGING_OBJECT_TYPE_PATTERN.matcher(type)).matches()) {
            return newPagingObject(spotifyPagingObject, matcher.group(1));
        } else if ((matcher = SpotifyWebApiUtils.CURSOR_PAGING_OBJECT_TYPE_PATTERN.matcher(type)).matches()) {
            return newPagingObject(spotifyCursorPagingObject, matcher.group(1));
        } else if (type.contains(" | ")) {
            return generateUnion(type);
        } else {
            type = type.replace("Simplified", "");
            addToQueue(type);
            return GraphQLTypeReference.typeRef(toGraphQLName(type));
        }
    }

    private GraphQLTypeReference generateUnion(String unionType) {
        var graphQLUnionType = (GraphQLUnionType) graphQLTypes.computeIfAbsent(unionType, type -> {
            var unions = Arrays.asList(type.split(" \\| "));
            var builder = GraphQLUnionType.newUnionType()
                .name("Union" + unions.stream().map(this::toGraphQLName).collect(Collectors.joining("")));

            unions.stream()
                .map(this::toGraphQLType)
                .map(t -> {
                    if (t instanceof GraphQLTypeReference) {
                        return (GraphQLTypeReference) t;
                    }
                    throw new RuntimeException("Union must only contain type references, but contained " + t.getClass().getName());
                })
                .forEach(builder::possibleType);
            return builder.build();
        });
        return GraphQLTypeReference.typeRef(graphQLUnionType.getName());
    }

    private GraphQLTypeReference newPagingObject(SpotifyWebApiObject baseSpotifyPagingObject, String itemType) {
        var key = baseSpotifyPagingObject.getName() + "Object[" + itemType + "]";
        var graphQLObject = (GraphQLObjectType)graphQLTypes.computeIfAbsent(key, s -> {
            var itemsProperty = baseSpotifyPagingObject.getProperties().stream()
                .filter(p -> "items".equals(p.getName()))
                .findFirst().orElseThrow();

            // Default type is Array[Object], replace with actual itemType, generate GraphQLType and then revert itemType change
            var defaultItemType = itemsProperty.getType();
            itemsProperty.setType("Array[" + itemType + "]");

            var graphQLPagingObject = generateObject(baseSpotifyPagingObject);

            itemsProperty.setType(defaultItemType);

            return GraphQLObjectType.newObject(graphQLPagingObject)
                .name(graphQLPagingObject.getName() + toGraphQLName(itemType))
                .build();
        });

        return GraphQLTypeReference.typeRef(graphQLObject.getName());
    }

    private void addToQueue(String object) {
        if (!graphQLTypes.containsKey(object)) {
            queue.add(object);
        }
    }

    private String toGraphQLName(String spotifyObjectName) {
        return spotifyObjectName.replace("Object", "").replace("Simplified", "");
    }
}
