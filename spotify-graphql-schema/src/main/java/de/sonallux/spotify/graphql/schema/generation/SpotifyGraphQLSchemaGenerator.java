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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.google.common.base.CaseFormat.*;
import static de.sonallux.spotify.core.model.SpotifyWebApiEndpoint.ParameterLocation.*;
import static graphql.Scalars.*;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLTypeReference.typeRef;

@Slf4j
public class SpotifyGraphQLSchemaGenerator {
    private static final List<String> ARGUMENTS_TO_IGNORE = List.of("market", "fields", "additional_types");

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
        schemaBuilder.mutation(generateMutationObject(spotifyWebApi));

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
                .map(field -> generateQueryObjectFieldDefinition(schemaObject, field))
                .collect(Collectors.toList()))
            .build();
    }

    private GraphQLFieldDefinition generateQueryObjectFieldDefinition(SchemaObject schemaObject, SchemaField field) {
        if (field.getEndpoint() != null) {
            return generateFieldDefinition(schemaObject, field);
        }
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
                .dataFetcher(coordinates(schemaObject.getName(), field.getName()), new BaseObjectsDataFetcher(baseName));
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
                .dataFetcher(coordinates(schemaObject.getName(), field.getName()), new BaseObjectDataFetcher(field.getName()));
        }

        return builder.build();
    }

    private GraphQLObjectType generateMutationObject(SpotifyWebApi spotifyWebApi) {
        var mutationBuilder = newObject().name("Mutation");

        spotifyWebApi.getCategoryList().stream().flatMap(c -> c.getEndpointList().stream())
            .filter(e -> !"GET".equals(e.getHttpMethod()))
            .forEach(endpoint -> {
                var fieldDefinition = generateMutationFieldDefinition(endpoint);
                var dataFetcher = MutationEndpointDataFetcher.factory(endpoint);
                codeRegistryBuilder.dataFetcher(coordinates("Mutation", fieldDefinition.getName()), dataFetcher);
                mutationBuilder.field(fieldDefinition);
            });

        return mutationBuilder.build();
    }

    private GraphQLFieldDefinition generateMutationFieldDefinition(SpotifyWebApiEndpoint endpoint) {
        var mutationName = LOWER_HYPHEN.to(LOWER_CAMEL, endpoint.getId().replace("endpoint-", ""));

        var inputTypeBuilder = newInputObject().name(mutationName + "Input");

        endpoint.getParameters().stream()
            .filter(p -> p.getLocation() != HEADER)
            .map(p -> {
                var inputType = getGraphQLInputType(p.getType());
                if (inputType == null) {
                    log.warn("Parameter {} of endpoint {} can not be mapped to a GraphQLInputType: {}", p.getName(), endpoint.getId(), p.getType());
                    return null;
                }

                return newInputObjectField()
                    .name(p.getName())
                    .description(p.getDescription())
                    .type(wrapInputType(inputType, p.isRequired()));
            })
            .filter(Objects::nonNull)
            .forEach(inputTypeBuilder::field);

        var payloadTypeBuilder = newObject()
            .name(mutationName.substring(0, 1).toUpperCase() + mutationName.substring(1) + "Payload")
            .field(newFieldDefinition().name("status").type(GraphQLInt));
        endpoint.getResponseTypes().stream()
            .filter(r -> !"Void".equals(r.getType()))
            .map(r -> {
                var graphQLObjectName = toGraphQLObjectName(r.getType());
                var fieldName = UPPER_CAMEL.to(LOWER_UNDERSCORE, graphQLObjectName);
                return newFieldDefinition().name(fieldName).type(typeRef(graphQLObjectName));
            })
            .forEach(payloadTypeBuilder::field);

        return newFieldDefinition()
            .name(mutationName)
            .description(endpoint.getDescription())
            .argument(newArgument().name("input").type(nonNull(inputTypeBuilder.build())))
            .type(payloadTypeBuilder)
            .build();
    }

    private GraphQLObjectType generateObject(SchemaObject schemaObject) {
        return newObject()
            .name(toGraphQLObjectName(schemaObject.getName()))
            .description(schemaObject.getDescription())
            .fields(schemaObject.getFields().values().stream()
                .map(field -> generateFieldDefinition(schemaObject, field))
                .collect(Collectors.toList()))
            .build();
    }

    private GraphQLFieldDefinition generateFieldDefinition(SchemaObject schemaObject, SchemaField schemaField) {
        registerDataFetcherForField(schemaObject, schemaField);

        var builder = newFieldDefinition()
            .name("type".equals(schemaField.getName()) ? "spotify_type" : schemaField.getName())
            .type(toGraphQLType(schemaField.getType()));

        if (schemaField.getDescription() != null) {
            builder.description(schemaField.getDescription().replace("\"", "'"));
        }

        var endpoint = schemaField.getEndpoint();
        if (endpoint != null) {
            for (var parameter : endpoint.getParameters()) {
                if (parameter.getLocation() == HEADER) {
                    continue;
                }
                if (parameter.getLocation() == PATH && schemaField.isIdProvidedByParent()) {
                    continue;
                }
                if (parameter.getLocation() == QUERY && ARGUMENTS_TO_IGNORE.contains(parameter.getName())) {
                    continue;
                }
                builder.argument(newArgument()
                    .name(parameter.getName())
                    .description(parameter.getDescription())
                    .type(wrapInputType(getGraphQLInputType(parameter.getType()), parameter.isRequired()))
                );
            }
        }

        return builder.build();
    }

    private GraphQLInputType wrapInputType(GraphQLInputType type, boolean isRequired) {
        if (isRequired) {
            return nonNull(type);
        }
        return type;
    }

    private void registerDataFetcherForField(SchemaObject schemaObject, SchemaField schemaField) {
        var endpoint = schemaField.getEndpoint();
        if (endpoint == null) {
            return;
        }

        var coords = coordinates(toGraphQLObjectName(schemaObject.getName()), schemaField.getName());
        var dataFetcher = new EndpointDataFetcher(endpoint, schemaField.getFieldExtraction(), schemaField.isIdProvidedByParent());
        codeRegistryBuilder.dataFetcher(coords, dataFetcher);
    }

    private GraphQLOutputType toGraphQLType(String type) {
        Matcher matcher;
        if ("Boolean".equals(type)) {
            return GraphQLBoolean;
        } else if ("Float".equals(type) || "Number".equals(type)) {
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
        Matcher matcher;
        if ("Boolean".equals(type)) {
            return GraphQLBoolean;
        } else if ("Float".equals(type) || "Number".equals(type)) {
            return GraphQLFloat;
        } else if ("Integer".equals(type)) {
            return GraphQLInt;
        } else if ("String".equals(type)) {
            return GraphQLString;
        } else if ("Timestamp".equals(type)) {
            return GraphQLString;
        } else if ((matcher = SpotifyWebApiUtils.ARRAY_TYPE_PATTERN.matcher(type)).matches()) {
            var itemType = getGraphQLInputType(matcher.group(1));
            return list(itemType);
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
