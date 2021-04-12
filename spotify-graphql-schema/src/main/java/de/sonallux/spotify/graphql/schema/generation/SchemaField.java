package de.sonallux.spotify.graphql.schema.generation;

import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
class SchemaField {
    private final String name;
    private final String type;
    private String description;
    private SpotifyWebApiEndpoint endpoint;
    private String fieldExtraction;

    SchemaField(String name, String type) {
        this(name, type, null, null, null);
    }

    SchemaField(String name, String type, String description) {
        this(name, type, description, null, null);
    }
}
