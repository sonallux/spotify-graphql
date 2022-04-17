package de.sonallux.spotify.graphql.wiring;

import graphql.schema.DataFetcher;
import graphql.schema.TypeResolver;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.UnionWiringEnvironment;
import graphql.schema.idl.WiringFactory;

public class SpotifyWiringFactory implements WiringFactory {
    private final SpotifyObjectTypeResolver spotifyObjectTypeResolver = new SpotifyObjectTypeResolver();

    @Override
    public DataFetcher<?> getDefaultDataFetcher(FieldWiringEnvironment env) {
        return SpotifyPropertyDataFetcher.fetching(env.getFieldDefinition().getName());
    }

    @Override
    public boolean providesTypeResolver(UnionWiringEnvironment env) {
        return spotifyObjectTypeResolver.canResolveUnionType(env.getUnionTypeDefinition());
    }

    @Override
    public TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
        return spotifyObjectTypeResolver;
    }
}
