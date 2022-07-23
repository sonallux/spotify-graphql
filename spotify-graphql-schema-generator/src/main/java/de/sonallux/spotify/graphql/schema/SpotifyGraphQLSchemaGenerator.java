package de.sonallux.spotify.graphql.schema;

import graphql.language.IntValue;
import graphql.schema.*;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLScalarType.newScalar;
import static graphql.schema.GraphQLTypeReference.typeRef;
import static java.util.stream.Collectors.*;

@RequiredArgsConstructor
public class SpotifyGraphQLSchemaGenerator {
    private static final Path OPEN_API_FILE = Path.of("spotify-graphql-schema-generator", "fixed-spotify-open-api.yml");
    private static final Path OUTPUT_FOLDER = Path.of("spotify-graphql-server/src/main/resources/graphql");

    private static final Set<String> IGNORED_FIELDS = Set.of("market", "additional_types", "fields");
    private static final GraphQLScalarType GRAPHQL_DUMMY_TYPE = newScalar().name("DUMMY_TYPE").coercing(GraphQLString.getCoercing()).build();

    private final SpotifyOpenApi spotifyOpenApi;

    private Map<String, MappedType> graphQLObjects = new HashMap<>();
    private Map<List<String>, GraphQLUnionType> unionTypes = new HashMap<>();
    private Map<Mapping.Category, GraphQLObjectType> queryTypes = new HashMap<>();
    private Queue<Mapping> workList = new LinkedList<>();

    public void generateGraphQLTypes() {
        graphQLObjects = new HashMap<>();
        unionTypes = new HashMap<>();
        queryTypes = new HashMap<>();
        workList = new LinkedList<>(TypeMappings.ROOT_TYPE_MAPPINGS);

        while (!workList.isEmpty()) {
            var element = workList.remove();

            if (element instanceof TypeMapping typeMapping) {
                if (!graphQLObjects.containsKey(typeMapping.openApiName())) {
                    handleTypeMapping(typeMapping);
                }
            } else if (element instanceof FieldMapping fieldMapping) {
                handleFieldMapping(fieldMapping);
            } else if (element instanceof BaseTypeQueryMapping baseTypeQueryMapping) {
                handleBaseTypeQueryMapping(baseTypeQueryMapping);
            } else if (element instanceof EmptyObjectQueryMapping emptyObjectQueryMapping) {
                handleEmptyObjectQueryMapping(emptyObjectQueryMapping);
            } else {
                // TODO Once switch with pattern matching is out of preview this is unnecessary
                throw new UnsupportedOperationException("Unknown mapping: " + element);
            }
        }
    }

    private void handleEmptyObjectQueryMapping(EmptyObjectQueryMapping emptyObjectQueryMapping) {
        transformQueryObject(emptyObjectQueryMapping.category(), builder -> builder.field(emptyObjectQueryMapping.fieldDefinition()));

        graphQLObjects.computeIfAbsent(emptyObjectQueryMapping.objectName(), name -> new MappedType(emptyObjectQueryMapping.category(), name));
    }

    private void handleBaseTypeQueryMapping(BaseTypeQueryMapping baseTypeQueryMapping) {
        transformQueryObject(baseTypeQueryMapping.category(), builder -> builder.fields(baseTypeQueryMapping.fieldDefinitions()));

        handleTypeMapping(new TypeMapping(baseTypeQueryMapping.baseTypeOpenApiName(), baseTypeQueryMapping.category()));

        graphQLObjects.computeIfAbsent("QueryObject", openApiName -> new MappedType(Mapping.Category.CORE, openApiName));
    }

    private void transformQueryObject(Mapping.Category category, Consumer<GraphQLObjectType.Builder> builderConsumer) {
        queryTypes.compute(category, (ignore, queryType) -> {
            if (queryType == null) {
                queryType = GraphQLUtils.getGraphQLObject("QueryObject").build();
            }

            return queryType.transform(builderConsumer);
        });
    }

    private void handleTypeMapping(TypeMapping typeMapping) {
        var openApiSchema = spotifyOpenApi.getSchema(typeMapping.openApiName());

        var mappedType = new MappedType(typeMapping.category(), typeMapping.openApiName());

        if (openApiSchema instanceof ObjectSchema objectSchema) {
            mappedType = mappedType.withFields(mapProperties(objectSchema.getProperties(), typeMapping.category()));
        } else if (openApiSchema instanceof ComposedSchema composedSchema) {
            for (var innerSchema : composedSchema.getAllOf()) {
                if (innerSchema instanceof ObjectSchema innerObjectSchema) {
                    mappedType = mappedType.withFields(mapProperties(innerObjectSchema.getProperties(), typeMapping.category()));
                } else if (innerSchema.get$ref() != null) {
                    var properties = spotifyOpenApi.getSchemaFromRef(innerSchema.get$ref()).getProperties();
                    mappedType = mappedType.withFields(mapProperties(properties, typeMapping.category()));
                } else {
                    throw new IllegalArgumentException("Unknown inner schema: " + innerSchema);
                }
            }
        } else {
            throw new IllegalArgumentException("Can not create GraphQLObject for OpenApi schema: " + openApiSchema);
        }

        graphQLObjects.put(typeMapping.openApiName(), mappedType);
    }

    private void handleFieldMapping(FieldMapping fieldMapping) {
        var parameters = spotifyOpenApi.getParameters(fieldMapping);
        var responseSchema = spotifyOpenApi.getResponseSchema(fieldMapping);

        var fieldType = mapToOutputType(responseSchema, fieldMapping.category());
        if (fieldType == GRAPHQL_DUMMY_TYPE) {
            var responseTypeName = spotifyOpenApi.getResponseSchemaName(fieldMapping);
            var type = graphQLObjects.computeIfAbsent(responseTypeName, openApiName -> {
                var mappedType = new MappedType(fieldMapping.category(), responseTypeName);
                if (responseSchema instanceof ObjectSchema objectSchema) {
                    return mappedType.withFields(mapProperties(objectSchema.getProperties(), fieldMapping.category()));
                } else {
                    return null;
                }
            });
            if (type == null) {
                throw new IllegalArgumentException("Can not map response schema type");
            }
            fieldType = type.graphQLObject();
        }

        var field = newFieldDefinition()
            .name(fieldMapping.fieldName())
            .type(fieldType)
            .arguments(parameters.stream()
                .map(spotifyOpenApi::getParameter)
                .filter(fieldMapping::filterParameter)
                .filter(parameter -> !IGNORED_FIELDS.contains(parameter.getName()))
                .map(this::mapParameter)
                .toList()
            )
            .build();

        if (fieldMapping.isQueryMappingForCategory()) {
            queryTypes.compute(fieldMapping.category(), (openApiName, objectType) -> {
                if (objectType == null) {
                    objectType = GraphQLUtils.getGraphQLObject("QueryObject").build();
                }

                return objectType.transform(builder -> builder.field(field));
            });
        } else {
            graphQLObjects.compute(fieldMapping.openApiName(), (openApiName, mappedType) -> {
                if (mappedType == null) {
                    mappedType = new MappedType(fieldMapping.category(), openApiName);
                }

                return mappedType.withFields(List.of(field));
            });
        }
    }

    private GraphQLArgument mapParameter(Parameter parameter) {
        var type = mapToInputType(parameter.getSchema());
        var builder = newArgument()
            .name(parameter.getName())
            .type(parameter.getRequired() ? nonNull(type) : type)
            .description(parameter.getSchema().getDescription());

        if ("limit".equals(parameter.getName()) && parameter.getSchema() instanceof IntegerSchema intSchema && intSchema.getDefault() != null) {
            builder.defaultValueLiteral(IntValue.of(intSchema.getDefault().intValue()));
        }

        if ("offset".equals(parameter.getName()) && parameter.getSchema() instanceof IntegerSchema) {
            builder.defaultValueLiteral(IntValue.of(0));
        }

        return builder.build();
    }

    private List<GraphQLFieldDefinition> mapProperties(Map<String, Schema> properties, Mapping.Category category) {
        return properties.entrySet().stream()
            .map(entry -> createFieldDefinition(entry.getKey(), entry.getValue(), category))
            .toList();
    }

    private GraphQLFieldDefinition createFieldDefinition(String property, Schema<?> schema, Mapping.Category category) {
        return newFieldDefinition()
            .name(property)
            .type(mapToOutputType(schema, category))
            .description(schema.getDescription())
            .build();
    }

    private GraphQLInputType mapToInputType(Schema<?> schema) {
        if (schema instanceof NumberSchema) {
            return GraphQLFloat;
        } else if (schema instanceof IntegerSchema) {
            return GraphQLInt;
        } else if (schema instanceof StringSchema) {
            return GraphQLString;
        } else if (schema instanceof BooleanSchema) {
            return GraphQLBoolean;
        } else if (schema instanceof DateSchema || schema instanceof DateTimeSchema) {
            return GraphQLString;
        } else if (schema instanceof ArraySchema) {
            return list(mapToInputType(schema.getItems()));
        } else {
            throw new IllegalArgumentException("Can not map OpenApi schema to input type: " + schema);
        }
    }

    private GraphQLOutputType mapToOutputType(Schema<?> schema, Mapping.Category category) {
        if (schema instanceof NumberSchema) {
            return GraphQLFloat;
        } else if (schema instanceof IntegerSchema) {
            return GraphQLInt;
        } else if (schema instanceof StringSchema) {
            return GraphQLString;
        } else if (schema instanceof BooleanSchema) {
            return GraphQLBoolean;
        } else if (schema instanceof DateSchema || schema instanceof DateTimeSchema) {
            return GraphQLString;
        } else if (schema instanceof ArraySchema) {
            return list(mapToOutputType(schema.getItems(), category));
        } else if (schema instanceof ComposedSchema) {
            if (schema.getAllOf() != null) {
                if (schema.getAllOf().size() == 1) {
                    return getGraphQLTypeReference(schema.getAllOf().get(0).get$ref(), category);
                } else if (schema.getAllOf().size() == 2 && schema.getAllOf().get(0).get$ref().equals("#/components/schemas/PagingObject")) {
                    var pagingSchema = spotifyOpenApi.getSchemaFromRef("#/components/schemas/PagingObject");
                    Map<String, Schema> itemsProperties = schema.getAllOf().get(1).getProperties();
                    var itemsOpenApiName = SpotifyOpenApi.getSchemaName(itemsProperties.get("items").getItems().get$ref());

                    var mappedType = graphQLObjects.computeIfAbsent(itemsOpenApiName + "PagingObject", openApiName ->
                        new MappedType(category, openApiName)
                            .withFields(mapProperties(pagingSchema.getProperties(), category))
                            .withFields(mapProperties(itemsProperties, category)));
                    return typeRef(mappedType.graphQLObject().getName());
                }
            } else if (schema.getOneOf() != null) {
                var possibleTypes = schema.getOneOf().stream()
                    .map(s -> getGraphQLTypeReference(s.get$ref(), category))
                    .map(GraphQLTypeReference::getName)
                    .sorted()
                    .toList();

                return unionTypes.computeIfAbsent(possibleTypes, GraphQLUtils::createUnionType);
            }
            throw new IllegalArgumentException("Can not map OpenApi schema: " + schema);
        } else if (schema.get$ref() != null){
            return getGraphQLTypeReference(schema.get$ref(), category);
        } else if (schema instanceof ObjectSchema) {
            // Property `items` of PagingObject has this type so a dummy type is returned
            // TODO: add a check that this type is not present in the final GraphQl schema
            return GRAPHQL_DUMMY_TYPE;
        } else {
            throw new IllegalArgumentException("Can not map OpenApi schema to output type: " + schema);
        }
    }

    private GraphQLTypeReference getGraphQLTypeReference(String reference, Mapping.Category category) {
        var openApiName = SpotifyOpenApi.getSchemaName(reference);
        if (!graphQLObjects.containsKey(openApiName)) {
            workList.add(new TypeMapping(openApiName, category));
        }
        return typeRef(GraphQLUtils.getGraphQLName(openApiName));
    }

    private Map<Mapping.Category, List<GraphQLNamedType>> getTypeMap() {
        var typeMap = graphQLObjects.values().stream()
            .collect(groupingBy(MappedType::category, mapping(MappedType::graphQLType, toList())));

        typeMap.computeIfAbsent(Mapping.Category.CORE, ignore -> new ArrayList<>())
            .addAll(unionTypes.values());

        queryTypes.forEach((category, queryType) -> typeMap.computeIfAbsent(category, ignore -> new ArrayList<>()).add(queryType));

        return typeMap;
    }

    public void writeTypes(Path outputFolder) {
        var schemaWriter = new GraphQLSchemaWriter(outputFolder);
        schemaWriter.writeTypes(getTypeMap());
    }

    public static void main(String[] args) throws Exception {
        var openApi = SpotifyOpenApi.fromFile(OPEN_API_FILE);

        var generator = new SpotifyGraphQLSchemaGenerator(openApi);

        generator.generateGraphQLTypes();
        generator.writeTypes(OUTPUT_FOLDER);
    }
}
