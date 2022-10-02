package de.sonallux.spotify.graphql.wiring;

import graphql.TypeResolutionEnvironment;
import graphql.language.TypeName;
import graphql.language.UnionTypeDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

import java.util.List;
import java.util.Map;

public class SpotifyObjectTypeResolver implements TypeResolver {

    private static final List<String> SPOTIFY_BASE_OBJECT_TYPE_NAMES = List.of("Album", "Artist", "Audiobook", "Chapter", "Episode", "Playlist", "Show", "Track");

    public boolean canResolveUnionType(UnionTypeDefinition unionTypeDefinition) {
        return unionTypeDefinition.getMemberTypes().stream().allMatch(type -> {
            if (type instanceof TypeName typeName) {
                return SPOTIFY_BASE_OBJECT_TYPE_NAMES.contains(typeName.getName());
            }
            return false;
        });
    }

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
