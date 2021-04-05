package de.sonallux.spotify.graphql.schema;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
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
