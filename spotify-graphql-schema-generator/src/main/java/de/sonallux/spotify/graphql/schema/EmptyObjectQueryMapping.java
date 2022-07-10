package de.sonallux.spotify.graphql.schema;

import graphql.schema.GraphQLFieldDefinition;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLTypeReference.typeRef;

public record EmptyObjectQueryMapping(Category category, String fieldName, String objectName) implements Mapping {
    public GraphQLFieldDefinition fieldDefinition() {
        return newFieldDefinition().name(fieldName).type(typeRef(GraphQLUtils.getGraphQLName(objectName))).build();
    }
}
