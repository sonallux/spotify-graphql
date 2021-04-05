package de.sonallux.spotify.graphql.schema.loader;

import graphql.GraphqlErrorException;
import okhttp3.Request;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.Try;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

abstract class AbstractBatchLoader<K, V> implements BatchLoaderWithContext<K, Try<V>> {

    @Override
    public CompletionStage<List<Try<V>>> load(List<K> list, BatchLoaderEnvironment environment) {
        return CompletableFuture
            .supplyAsync(() -> loadBatch(list, environment));
    }

    protected abstract List<Try<V>> loadBatch(List<K> list, BatchLoaderEnvironment environment);

    protected GraphqlErrorException getGraphQLErrorException(IOException e) {
        return GraphqlErrorException.newErrorException().message(e.getMessage()).cause(e).build();
    }

    protected Request.Builder getRequestBuilder(BatchLoaderEnvironment environment) {
        Map<String, String> context = environment.getContext();
        return new Request.Builder().addHeader("Authorization", context.get("authorizationHeader"));
    }

    protected Try<Map<String, Object>> wrapSpotifyBaseObject(Map<String, Object> object) {
        if (object == null) {
            return Try.succeeded(null);
        }
        object.put("spotify_type", object.get("type"));
        return Try.succeeded(object);
    }
}
