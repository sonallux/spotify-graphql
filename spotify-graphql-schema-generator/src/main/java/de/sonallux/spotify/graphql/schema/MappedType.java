package de.sonallux.spotify.graphql.schema;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;

import java.util.List;

public record MappedType(Mapping.Category category, GraphQLObjectType graphQLObject) {

    public MappedType(Mapping.Category category, String openApiName) {
        this(category, GraphQLUtils.getGraphQLObject(openApiName).build());
    }

    public GraphQLNamedType graphQLType() {
        return graphQLObject;
    }

    public MappedType withFields(List<GraphQLFieldDefinition> fieldDefinitions) {
        var newObject = graphQLObject.transform(builder -> {
            for (var field : fieldDefinitions) {
                var existingField = graphQLObject.getFieldDefinition(field.getName());
                if (existingField == null) {
                    builder.field(field);
                } else {
                    builder.field(merge(existingField, field));
                }
            }
        });

        return new MappedType(category, newObject);
    }

    private static GraphQLFieldDefinition merge(GraphQLFieldDefinition first, GraphQLFieldDefinition second) {
        if (!first.getName().equals(second.getName())) {
            throw new IllegalArgumentException("Can not merge fields with different names");
        }

        return first.transform(builder -> {
            builder.type(second.getType());

            if (second.getDescription() != null) {
                builder.description(second.getDescription());
            }

            builder.arguments(second.getArguments());
        });
    }
}
