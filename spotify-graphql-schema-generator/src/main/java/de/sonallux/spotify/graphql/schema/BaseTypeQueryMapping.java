package de.sonallux.spotify.graphql.schema;

import com.google.common.base.Converter;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;

import java.util.List;

import static com.google.common.base.CaseFormat.*;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLTypeReference.typeRef;

public record BaseTypeQueryMapping(Mapping.Category category) implements Mapping {
    private static final Converter<String, String> CATEGORY_TO_OPEN_API_NAME = UPPER_UNDERSCORE.converterTo(UPPER_CAMEL);
    private static final Converter<String, String> CATEGORY_TO_FIELD_NAME = UPPER_UNDERSCORE.converterTo(LOWER_UNDERSCORE);


    public String baseTypeOpenApiName() {
        return CATEGORY_TO_OPEN_API_NAME.convert(category.name()) + "Object";
    }

    public List<GraphQLFieldDefinition> fieldDefinitions() {
        var type = typeRef(GraphQLUtils.getGraphQLName(baseTypeOpenApiName()));
        return List.of(fieldDefinitionSingleQuery(type), fieldDefinitionMultipleQuery(type));
    }

    private GraphQLFieldDefinition fieldDefinitionSingleQuery(GraphQLOutputType baseType) {
        return newFieldDefinition()
            .name(CATEGORY_TO_FIELD_NAME.convert(category.name()))
            .argument(newArgument()
                .name("id")
                .type(GraphQLString)
                .description("The Spotify ID of the object to query. Either `id` or `uri` must be specified"))
            .argument(newArgument()
                .name("uri")
                .type(GraphQLString)
                .description("The Spotify URI of the object to query. Either `id` or `uri` must be specified"))
            .type(baseType)
            .build();
    }

    private GraphQLFieldDefinition fieldDefinitionMultipleQuery(GraphQLOutputType baseType) {
        return newFieldDefinition()
            .name(CATEGORY_TO_FIELD_NAME.convert(category.name()) + "s")
            .argument(newArgument()
                .name("ids")
                .type(list(GraphQLString))
                .description("A list of Spotify IDs of the objects to query. Either `ids` or `uris` must be specified"))
            .argument(newArgument()
                .name("uris")
                .type(list(GraphQLString))
                .description("A list of Spotify URIs of the objects to query. Either `ids` or `uris` must be specified"))
            .type(list(baseType))
            .build();
    }
}
