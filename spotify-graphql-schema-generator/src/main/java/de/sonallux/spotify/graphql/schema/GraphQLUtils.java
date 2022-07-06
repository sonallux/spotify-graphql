package de.sonallux.spotify.graphql.schema;

import graphql.schema.GraphQLUnionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

import static graphql.schema.GraphQLObjectType.newObject;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GraphQLUtils {
    public static String getGraphQLName(String openApiName){
        return openApiName
            .replace("Object", "")
            .replace("Simplified", "");
    }

    public static GraphQLUnionType createUnionType(List<String> possibleTypes) {
        var name = "Union" + String.join("", possibleTypes);
        var builder = GraphQLUnionType.newUnionType().name(name);
        possibleTypes.forEach(type -> builder.possibleType(newObject().name(type).build()));
        return builder.build();
    }
}
