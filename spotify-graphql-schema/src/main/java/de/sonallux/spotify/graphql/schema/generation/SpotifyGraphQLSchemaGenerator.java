package de.sonallux.spotify.graphql.schema.generation;

import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import de.sonallux.spotify.graphql.SpotifyWebApiAdjuster;
import de.sonallux.spotify.graphql.schema.*;
import graphql.schema.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static graphql.Scalars.*;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLTypeReference.typeRef;

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
        return newObject()
            .name(objectName)
            .description(schemaObject.getDescription())
            .fields(schemaObject.getFields().values().stream()
                .map(field -> generateQueryObjectFieldDefinition(objectName, field))
                .collect(Collectors.toList()))
            .build();
    }

    private GraphQLFieldDefinition generateQueryObjectFieldDefinition(String objectName, SchemaField field) {
        var builder = newFieldDefinition()
            .name(field.getName())
            .type(toGraphQLType(field.getType()));

        if (field.getDescription() != null) {
            builder.description(field.getDescription().replace("\"", "'"));
        }

        if (field.getName().endsWith("s")) {
            builder
                .argument(newArgument()
                    .name("ids")
                    .description("A list of Spotify IDs of the objects to query. Either `ids` or `uris` must be specified")
                    .type(list(GraphQLString)))
                .argument(newArgument()
                    .name("uris")
                    .description("A list of Spotify URIs of the objects to query. Either `ids` or `uris` must be specified")
                    .type(list(GraphQLString)));
            var baseName = field.getName().substring(0, field.getName().length() - 1);
            codeRegistryBuilder
                .dataFetcher(coordinates(objectName, field.getName()), new BaseObjectsDataFetcher(baseName));
        } else {
            builder
                .argument(newArgument()
                    .name("id")
                    .description("The Spotify ID of the object to query. Either `id` or `uri` must be specified")
                    .type(GraphQLString))
                .argument(newArgument()
                    .name("uri")
                    .description("The Spotify URI of the object to query. Either `id` or `uri` must be specified")
                    .type(GraphQLString));
            codeRegistryBuilder
                .dataFetcher(coordinates(objectName, field.getName()), new BaseObjectDataFetcher(field.getName()));
        }

        return builder.build();
    }

    private GraphQLObjectType generateObject(SchemaObject schemaObject) {
        return newObject()
            .name(toGraphQLObjectName(schemaObject.getName()))
            .description(schemaObject.getDescription())
            .fields(schemaObject.getFields().values().stream()
                .peek(schemaField -> registerDataFetcherForField(schemaObject, schemaField))
                .map(this::generateFieldDefinition)
                .collect(Collectors.toList()))
            .build();
    }

    private GraphQLFieldDefinition generateFieldDefinition(SchemaField schemaField) {
        var builder = newFieldDefinition()
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
                .forEach(p -> builder.argument(newArgument()
                    .name(p.getName())
                    .description(p.getDescription())
                    .type(getGraphQLInputType(p.getType()))
                ));
        }

        return builder.build();
    }

    private void registerDataFetcherForField(SchemaObject schemaObject, SchemaField schemaField) {
        var endpoint = schemaField.getEndpoint();
        if (endpoint == null) {
            return;
        }

        var coords = coordinates(toGraphQLObjectName(schemaObject.getName()), schemaField.getName());
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
            return list(itemType);
        } else if (type.contains(" | ")) {
            return generateUnion(type);
        } else {
            return typeRef(toGraphQLObjectName(type));
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
            .map(t -> typeRef(toGraphQLObjectName(t)))
            .forEach(builder::possibleType);
        var graphQLUnionType = builder.build();
        schemaBuilder.additionalType(graphQLUnionType);
        codeRegistryBuilder.typeResolver(graphQLUnionType, new SpotifyObjectTypeResolver());

        return typeRef(graphQLUnionType.getName());
    }

    private String toGraphQLObjectName(String name) {
        return name.replace("Object", "");
    }
}
