package de.sonallux.spotify.graphql;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpotifyGraphQLSchemaPrinter {
    public static void main(String[] args) throws IOException {
        var spotifyGraphQL = new SpotifyGraphQL();
        Files.writeString(Path.of("spotify-graphql-schema/schema.graphql"), spotifyGraphQL.printSchema());
    }
}
