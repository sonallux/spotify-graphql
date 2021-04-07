package de.sonallux.spotify.graphql.schema;

import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.graphql.SpotifyWebApiAdjuster;
import graphql.schema.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.Scalars.GraphQLString;

public class SchemaCreator {
    private static final List<String> BASE_SPOTIFY_TYPES = List.of("AlbumObject", "ArtistObject", "EpisodeObject", "PlaylistObject", "PlaylistTrackObject", "TrackObject", "ShowObject");
    public static final List<String> BASE_TYPES = List.of("album", "artist", "episode", "playlist", "show", "track");

    public GraphQLSchema generate(SpotifyWebApi spotifyWebApi) {
        SpotifyWebApiAdjuster.adjust(spotifyWebApi);

        var schemaBuilder = GraphQLSchema.newSchema();

        var typeGenerator = new TypeGenerator(spotifyWebApi);
        BASE_SPOTIFY_TYPES.forEach(typeGenerator::getType);
        var graphQLTypes = typeGenerator.getAllTypes();
        graphQLTypes.forEach(schemaBuilder::additionalType);

        var codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
        generateUnionResolver(graphQLTypes).forEach(codeRegistryBuilder::typeResolver);
        codeRegistryBuilder.defaultDataFetcher(env -> SpotifyPropertyDataFetcher.fetching(env.getFieldDefinition().getName()));
        AdditionalFields.registerAdditionalDataFetcher(codeRegistryBuilder);

        schemaBuilder.query(buildQueryObject(codeRegistryBuilder));
        schemaBuilder.codeRegistry(codeRegistryBuilder.build());

        return schemaBuilder.build();
    }

    private GraphQLObjectType buildQueryObject(GraphQLCodeRegistry.Builder codeRegistryBuilder) {
        var objectBuilder = GraphQLObjectType.newObject().name("Query");

        BASE_TYPES.forEach(type -> {
            objectBuilder.field(GraphQLFieldDefinition.newFieldDefinition()
                .name(type)
                .argument(GraphQLArgument.newArgument().name("id").type(GraphQLString).build())
                .argument(GraphQLArgument.newArgument().name("uri").type(GraphQLString).build())
                .type(GraphQLTypeReference.typeRef(type.substring(0, 1).toUpperCase() + type.substring(1)))
            );
            codeRegistryBuilder.dataFetcher(FieldCoordinates.coordinates("Query", type), new BaseDataFetcher(type));
        });

        return objectBuilder.build();
    }

    private Map<String, TypeResolver> generateUnionResolver(Collection<GraphQLType> graphQLTypes) {
        return graphQLTypes.stream()
            .filter(type -> type instanceof GraphQLUnionType)
            .map(type -> (GraphQLUnionType)type)
            .collect(Collectors.toMap(GraphQLUnionType::getName, type -> new SpotifyObjectTypeResolver()));
    }
}
