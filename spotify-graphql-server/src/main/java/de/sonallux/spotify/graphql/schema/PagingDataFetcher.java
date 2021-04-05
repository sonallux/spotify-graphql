package de.sonallux.spotify.graphql.schema;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.DataFetchingEnvironment;
import lombok.AllArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@AllArgsConstructor
public class PagingDataFetcher implements DataFetcher<Object> {
    private final String dataLoaderName;

    public static DataFetcherFactory<Object> factory(String dataLoaderName) {
        return (env) -> new PagingDataFetcher(dataLoaderName);
    }

    @Override
    public Object get(DataFetchingEnvironment env) throws Exception {
        Map<?, ?> parentObject = env.getSource();
        Integer limit = env.getArgument("limit");
        Integer offset = env.getArgument("offset");

        //TODO: Maybe check if pagingObject has correct limit and offset and then return it immediately

        var requestData = new LinkedHashMap<String, Object>();
        requestData.put("id", parentObject.get("id"));
        if (limit != null) {
            requestData.put("limit", String.valueOf(limit));
        }
        if (offset != null) {
            requestData.put("offset", String.valueOf(offset));
        }

        return env.getDataLoader(dataLoaderName).load(requestData);
    }
}
