package de.sonallux.spotify.graphql.wiring;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.PropertyDataFetcher;

import java.util.Map;

public class SpotifyPropertyDataFetcher implements DataFetcher<Object> {

    private final PropertyDataFetcher<Object> defaultPropertyDataFetcher;

    private SpotifyPropertyDataFetcher(String propertyName) {
        this.defaultPropertyDataFetcher = PropertyDataFetcher.fetching(propertyName);
    }

    public static SpotifyPropertyDataFetcher fetching(String propertyName) {
        return new SpotifyPropertyDataFetcher(propertyName);
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        var data = defaultPropertyDataFetcher.get(environment);
        if (data != null) {
            return data;
        }
        //Property might not be present because it is a simplified object
        //Try to load the full object
        if (environment.getSource() instanceof Map<?, ?> parentObject) {
            var id = (String) parentObject.get("id");
            var type = (String) parentObject.get("type");
            if (id != null && type != null) {
                return environment.getDataLoader(type + "Loader")
                    .load(id)
                    .handle((fullObject, error) -> {
                        if (fullObject instanceof Map) {
                            return ((Map<?, ?>) fullObject).get(environment.getFieldDefinition().getName());
                        }
                        return null;
                    });
            }
        }
        return null;
    }
}
