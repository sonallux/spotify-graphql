package de.sonallux.spotify.graphql.schema;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

import java.util.Map;

public class SpotifyObjectTypeResolver implements TypeResolver {
    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        Map<?, ?> object = env.getObject();
        var spotifyType = (String) object.get("type");
        if (spotifyType == null) {
            return null;
        }
        var graphQLType = spotifyType.substring(0, 1).toUpperCase() + spotifyType.substring(1);
        return (GraphQLObjectType)env.getSchema().getType(graphQLType);
    }
}
