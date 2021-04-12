package de.sonallux.spotify.graphql.schema.loader;

import graphql.GraphqlErrorException;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.Try;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

abstract class AbstractBatchLoader<K> implements BatchLoaderWithContext<K, Try<?>> {

    @Override
    public CompletionStage<List<Try<?>>> load(List<K> list, BatchLoaderEnvironment environment) {
        return CompletableFuture
            .supplyAsync(() -> loadBatch(list, environment));
    }

    protected abstract List<Try<?>> loadBatch(List<K> list, BatchLoaderEnvironment environment);

    protected GraphqlErrorException getGraphQLErrorException(IOException e) {
        return GraphqlErrorException.newErrorException().message(e.getMessage()).cause(e).build();
    }

    protected Request.Builder getRequestBuilder(BatchLoaderEnvironment environment) {
        Map<String, String> context = environment.getContext();
        return new Request.Builder().addHeader("Authorization", context.get("authorizationHeader"));
    }

    protected HttpUrl.Builder getUrlBuilder(BatchLoaderEnvironment environment) {
        Map<String, String> context = environment.getContext();
        return HttpUrl.get(context.get("baseUrl")).newBuilder();
    }
}
