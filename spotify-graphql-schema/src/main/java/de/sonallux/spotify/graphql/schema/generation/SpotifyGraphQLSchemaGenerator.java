package de.sonallux.spotify.graphql.schema.generation;

import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import de.sonallux.spotify.graphql.SpotifyWebApiAdjuster;
import de.sonallux.spotify.graphql.schema.BaseDataFetcher;
import de.sonallux.spotify.graphql.schema.EndpointDataFetcher;
import de.sonallux.spotify.graphql.schema.SpotifyObjectTypeResolver;
import de.sonallux.spotify.graphql.schema.SpotifyPropertyDataFetcher;
import graphql.schema.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static graphql.Scalars.*;

@Slf4j
public class SpotifyGraphQLSchemaGenerator {
    private static final List<String> ARGUMENTS_TO_ADD = List.of("limit", "offset");

    private final GraphQLSchema.Builder schemaBuilder;
    private final GraphQLCodeRegistry.Builder codeRegistryBuilder;

    public SpotifyGraphQLSchemaGenerator() {
        this.schemaBuilder = GraphQLSchema.newSchema();
        this.codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
    }

    public GraphQLSchema generate(SpotifyWebApi spotifyWebApi) {
        SpotifyWebApiAdjuster.adjust(spotifyWebApi);

        var schemaObjectMap = new SchemaObjectMapper(spotifyWebApi).generate();

        var queryObject = schemaObjectMap.remove("Query");
        schemaBuilder.query(generateQueryObject(queryObject));

        codeRegistryBuilder.defaultDataFetcher(env -> SpotifyPropertyDataFetcher.fetching(env.getFieldDefinition().getName()));

        schemaObjectMap.forEach((name, schemaObject) -> schemaBuilder.additionalType(generateObject(schemaObject)));

        return schemaBuilder.codeRegistry(codeRegistryBuilder.build()).build();
    }

    private GraphQLObjectType generateQueryObject(SchemaObject schemaObject) {
        var objectName = toGraphQLObjectName(schemaObject.getName());
        return GraphQLObjectType.newObject()
            .name(objectName)
            .description(schemaObject.getDescription())
            .fields(schemaObject.getFields().values().stream()
                .map(this::generateFieldDefinition)
                .map(builder -> builder
                    .argument(GraphQLArgument.newArgument()
                        .name("id")
                        .description("The Spotify ID of the object to query. Either `id` or `uri` must be specified")
                        .type(GraphQLString))
                    .argument(GraphQLArgument.newArgument()
                        .name("uri")
                        .description("The Spotify URI of the object to query. Either `id` or `uri` must be specified")
                        .type(GraphQLString))
                )
                .map(GraphQLFieldDefinition.Builder::build)
                .peek(field -> codeRegistryBuilder
                    .dataFetcher(FieldCoordinates.coordinates(objectName, field.getName()), new BaseDataFetcher(field.getName())))
                .collect(Collectors.toList()))
            .build();
    }

    private GraphQLObjectType generateObject(SchemaObject schemaObject) {
        return GraphQLObjectType.newObject()
            .name(toGraphQLObjectName(schemaObject.getName()))
            .description(schemaObject.getDescription())
            .fields(schemaObject.getFields().values().stream()
                .peek(schemaField -> registerDataFetcherForField(schemaObject, schemaField))
                .map(this::generateFieldDefinition)
                .map(GraphQLFieldDefinition.Builder::build)
                .collect(Collectors.toList()))
            .build();
    }

    private GraphQLFieldDefinition.Builder generateFieldDefinition(SchemaField schemaField) {
        var builder = GraphQLFieldDefinition.newFieldDefinition()
            .name("type".equals(schemaField.getName()) ? "spotify_type" : schemaField.getName())
            .type(toGraphQLType(schemaField.getType()));

        if (schemaField.getDescription() != null) {
            builder.description(schemaField.getDescription().replace("\"", "'"));
        }

        var endpoint = schemaField.getEndpoint();
        if (endpoint != null) {
            endpoint.getParameters().stream()
                .filter(p -> p.getLocation() == SpotifyWebApiEndpoint.ParameterLocation.QUERY)
                .filter(p -> ARGUMENTS_TO_ADD.contains(p.getName()))
                .forEach(p -> builder.argument(GraphQLArgument.newArgument()
                    .name(p.getName())
                    .description(p.getDescription())
                    .type(getGraphQLInputType(p.getType()))
                ));
        }

        return builder;
    }

    private void registerDataFetcherForField(SchemaObject schemaObject, SchemaField schemaField) {
        var endpoint = schemaField.getEndpoint();
        if (endpoint == null) {
            return;
        }

        var coords = FieldCoordinates.coordinates(toGraphQLObjectName(schemaObject.getName()), schemaField.getName());
        var dataFetcher = new EndpointDataFetcher(endpoint, schemaField.getFieldExtraction());
        codeRegistryBuilder.dataFetcher(coords, dataFetcher);
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
        } else if (type.contains(" | ")) {
            return generateUnion(type);
        } else {
            return GraphQLTypeReference.typeRef(toGraphQLObjectName(type));
        }
    }

    private static GraphQLInputType getGraphQLInputType(String type) {
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

    private GraphQLTypeReference generateUnion(String unionType) {
        var unions = Arrays.asList(unionType.split(" \\| "));
        var builder = GraphQLUnionType.newUnionType()
            .name("Union" + unions.stream().map(this::toGraphQLObjectName).collect(Collectors.joining("")));

        unions.stream()
            .map(t -> GraphQLTypeReference.typeRef(toGraphQLObjectName(t)))
            .forEach(builder::possibleType);
        var graphQLUnionType = builder.build();
        schemaBuilder.additionalType(graphQLUnionType);
        codeRegistryBuilder.typeResolver(graphQLUnionType, new SpotifyObjectTypeResolver());

        return GraphQLTypeReference.typeRef(graphQLUnionType.getName());
    }

    private String toGraphQLObjectName(String name) {
        return name.replace("Object", "");
    }
}
