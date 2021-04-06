package de.sonallux.spotify.graphql.schema;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.DataFetchingEnvironment;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class DelegateToLoaderDataFetcher implements DataFetcher<Object> {
    private final String dataLoaderName;

    public static DataFetcherFactory<Object> factory(String dataLoaderName) {
        return (env) -> new DelegateToLoaderDataFetcher(dataLoaderName);
    }

    @Override
    public Object get(DataFetchingEnvironment env) throws Exception {
        Map<?, ?> parentObject = env.getSource();
        var id = (String) parentObject.get("id");
        return env.getDataLoader(dataLoaderName).load(id);
    }
}
